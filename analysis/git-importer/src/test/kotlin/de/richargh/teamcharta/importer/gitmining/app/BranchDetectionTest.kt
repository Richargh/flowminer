package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.git.app.aGitLog
import de.richargh.teamcharta.importer.git.app.api.hash
import de.richargh.teamcharta.importer.gitmining.app.api.aBranch
import de.richargh.teamcharta.importer.shared.time.app.atStartOfYear
import de.richargh.teamcharta.importer.shared.time.app.zoned
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BranchDetectionTest {

    @Nested
    inner class LastCommitTracking {

        @Test
        fun `should set lastCommit equal to firstCommit for single commit branch`() {
            // main: 0 (origin/main)

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2024))
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches["origin/main"] shouldBe aBranch {
                name("origin/main")
                firstCommitHash("0".hash())
                firstCommitDate(atStartOfYear(2024))
                lastCommitHash("0".hash())
                lastCommitDate(atStartOfYear(2024))
            }
        }

        @Test
        fun `should track lastCommit as most recent commit on branch`() {
            // main: 0───1───2 (origin/main)

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2023))
                }
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2024))
                }
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2025))
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches["origin/main"] shouldBe aBranch {
                name("origin/main")
                firstCommitHash("0".hash())
                firstCommitDate(atStartOfYear(2023))
                intermediateCommits("1".hash())
                lastCommitHash("2".hash())
                lastCommitDate(atStartOfYear(2025))
            }
        }

        @Test
        fun `should track lastCommit for merged branch`() {
            // main:    0───2 (origin/main) [merge feature-login]
            //           \ /
            // feature:   1 (origin/feature-login)

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2023))
                }
                anEntry("origin/feature-login", "origin/main") {
                    authorDate(atStartOfYear(2024))
                }
                anEntry("origin/main", "origin/feature-login") {
                    authorDate(atStartOfYear(2025))
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches["origin/feature-login"] shouldBe aBranch {
                name("origin/feature-login")
                firstCommitHash("1".hash())
                firstCommitDate(atStartOfYear(2024))
                lastCommitHash("1".hash())
                lastCommitDate(atStartOfYear(2024))
                mergedInto("origin/main", atStartOfYear(2025), "2")
            }
        }

        @Test
        fun `should track lastCommit for branch with multiple commits before merge`() {
            // trunk: 0───1───────4 (origin/trunk) [merge feat]
            //         \         /
            // feat:    └─2───3─┘ (origin/feat)

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/trunk") {
                    authorDate(atStartOfYear(2021))
                }
                anEntry("origin/trunk") {
                    authorDate(atStartOfYear(2022))
                }
                anEntry("origin/feat", "origin/trunk") {
                    authorDate(atStartOfYear(2023))
                }
                anEntry("origin/feat") {
                    authorDate(atStartOfYear(2024))
                }
                anEntry("origin/trunk", "origin/feat") {
                    authorDate(atStartOfYear(2025))
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches["origin/feat"] shouldBe aBranch {
                name("origin/feat")
                firstCommitHash("2".hash())
                firstCommitDate(atStartOfYear(2023))
                lastCommitHash("3".hash())
                lastCommitDate(atStartOfYear(2024))
                mergedInto("origin/trunk", atStartOfYear(2025), "4")
            }
        }

        @Test
        fun `should track lastCommit for inferred branch`() {
            // trunk: 0───1───────4 (HEAD -> trunk, origin/trunk) [merge feat]
            //         \         /
            // feat:    └─2───3─┘ (deleted, inferred from merge message)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|4|1 3|2025-01-01T00:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat' into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3|2|2024-01-01T00:00:00+01:00|John Doe|john@example.com|feat 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat2.md

            -----COMMIT_START-----
            |2|0|2023-01-01T00:00:00+01:00|John Doe|john@example.com|feat 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat.md

            -----COMMIT_START-----
            |1|0|2022-01-01T00:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk2.md

            -----COMMIT_START-----
            |0||2021-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches["origin/feat"] shouldBe aBranch {
                inferredName("origin/feat")
                firstCommitHash("2".hash())
                firstCommitDate(atStartOfYear(2023))
                lastCommitHash("3".hash())
                lastCommitDate(atStartOfYear(2024))
                mergedInto("origin/trunk", atStartOfYear(2025), "4")
            }
        }

        @Test
        fun `should track lastCommit for unnamed branch`() {
            // trunk: 0───1───────4 (HEAD -> trunk, origin/trunk) [merge with custom message]
            //         \         /
            // ???:     └─2───3─┘ (deleted, cannot infer name)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|4|1 3|2025-01-01T00:00:00+01:00|John Doe|john@example.com|Squashed feature commits
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3|2|2024-01-01T00:00:00+01:00|John Doe|john@example.com|feat 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat2.md

            -----COMMIT_START-----
            |2|0|2023-01-01T00:00:00+01:00|John Doe|john@example.com|feat 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat.md

            -----COMMIT_START-----
            |1|0|2022-01-01T00:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk2.md

            -----COMMIT_START-----
            |0||2021-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.unnamed shouldHaveSize 1
            result.branches.unnamed.first() shouldBe aBranch {
                unNamed()
                firstCommitHash("2".hash())
                firstCommitDate(atStartOfYear(2023))
                lastCommitHash("3".hash())
                lastCommitDate(atStartOfYear(2024))
                mergedInto("origin/trunk", atStartOfYear(2025), "4")
            }
        }
    }

    @Test
    fun `should extract branch info for initial commit`() {
        // main: 0 (origin/main)

        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                authorDate(atStartOfYear(2024))
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 1
        result.branches["origin/main"] shouldBe aBranch {
            name("origin/main")
            firstCommitHash("0".hash())
            firstCommitDate(atStartOfYear(2024))
            lastCommitHash("0".hash())
            lastCommitDate(atStartOfYear(2024))
        }
    }

    @Test
    fun `should extract branch info for multiple commits on branch`() {
        // main: 0───1───2 (origin/main)

        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                authorDate(atStartOfYear(2023))
            }
            anEntry("origin/main") {
                authorDate(atStartOfYear(2024))
            }
            anEntry("origin/main") {
                authorDate(atStartOfYear(2025))
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 1
        result.branches["origin/main"] shouldBe aBranch {
            name("origin/main")
            firstCommitHash("0".hash())
            firstCommitDate(atStartOfYear(2023))
            intermediateCommits("1".hash())
            lastCommitHash("2".hash())
            lastCommitDate(atStartOfYear(2025))
        }
    }

    @Test
    fun `should extract branch info from merge commits`() {
        // main:    0───2 (origin/main) [merge feature-login]
        //           \ /
        // feature:   1 (origin/feature-login)

        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                authorDate(atStartOfYear(2023))
            }
            anEntry("origin/feature-login", "origin/main") {
                authorDate(atStartOfYear(2024))
            }
            anEntry("origin/main", "origin/feature-login") {
                authorDate(atStartOfYear(2025))
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 2
        result.branches["origin/main"] shouldBe aBranch {
            name("origin/main")
            firstCommitHash("0".hash())
            firstCommitDate(atStartOfYear(2023))
            lastCommitHash("2".hash())
            lastCommitDate(atStartOfYear(2025))
        }
        result.branches["origin/feature-login"] shouldBe aBranch {
            name("origin/feature-login")
            firstCommitHash("1".hash())
            firstCommitDate(atStartOfYear(2024))
            lastCommitHash("1".hash())
            lastCommitDate(atStartOfYear(2024))
            mergedInto("origin/main", atStartOfYear(2025), "2")
        }
    }

    @Test
    fun `should extract branch info when one branch is unmerged`() {
        // main:           0─────3 (origin/main) [merge feature-login]
        //                 │    /
        // feature-login:  ├─1─┘ (origin/feature-login)
        //                 │
        // feature-user:   └─2 (origin/feature-user, unmerged)

        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                authorDate(atStartOfYear(2022))
            }
            anEntry("origin/feature-login", "origin/main") {
                authorDate(atStartOfYear(2023))
            }
            anEntry("origin/feature-user", "origin/main") {
                authorDate(atStartOfYear(2024))
            }
            anEntry("origin/main", "origin/feature-login") {
                authorDate(atStartOfYear(2025))
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 3
        result.branches["origin/main"] shouldBe aBranch {
            name("origin/main")
            firstCommitHash("0".hash())
            firstCommitDate(atStartOfYear(2022))
            lastCommitHash("3".hash())
            lastCommitDate(atStartOfYear(2025))
        }
        result.branches["origin/feature-login"] shouldBe aBranch {
            name("origin/feature-login")
            firstCommitHash("1".hash())
            firstCommitDate(atStartOfYear(2023))
            lastCommitHash("1".hash())
            lastCommitDate(atStartOfYear(2023))
            mergedInto("origin/main", atStartOfYear(2025), "3")
        }
        result.branches["origin/feature-user"] shouldBe aBranch {
            name("origin/feature-user")
            firstCommitHash("2".hash())
            firstCommitDate(atStartOfYear(2024))
            lastCommitHash("2".hash())
            lastCommitDate(atStartOfYear(2024))
        }
    }

    @Test
    fun `should track first and last commits by date even when branch has many commits`() {
        // Long-running branch with many commits
        // trunk:   0─────────────────────6 (origin/trunk) [merge feat]
        //           \                   /
        // feat:      └─1───2───3───4───5 (origin/feat)

        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|6|0 5|2025-01-01T06:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat' into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat|5|4|2025-01-01T05:00:00+01:00|John Doe|john@example.com|feat commit 5
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat5.md

            -----COMMIT_START-----
            |4|3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|feat commit 4
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat4.md

            -----COMMIT_START-----
            |3|2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat commit 3
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat3.md

            -----COMMIT_START-----
            |2|1|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat commit 2
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat2.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|feat commit 1
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat1.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
        """.trimIndent()

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 2
        result.branches["origin/feat"] shouldBe aBranch {
            name("origin/feat")
            firstCommitHash("1".hash())
            firstCommitDate("2025-01-01T01:00:00+01:00".zoned())
            intermediateCommits("2".hash(), "3".hash(), "4".hash())
            lastCommitHash("5".hash())
            lastCommitDate("2025-01-01T05:00:00+01:00".zoned())
            mergedInto("origin/trunk", "2025-01-01T06:00:00+01:00".zoned(), "6")
        }
    }

    @Nested
    inner class OctopusMerges {

        @Test
        fun `should handle octopus merge with three parents and branch names from tips`() {
            // trunk: 0───1───────────5 (HEAD -> trunk, origin/trunk) [octopus merge feat-a and feat-b]
            //         \    \        /
            // feat-a:  └─2──\──────/ (deleted, inferred from merge message)
            //                \    /
            // feat-b:         └─3-4 (deleted, inferred from merge message)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|5|1 2 4|2025-01-01T05:00:00+01:00|John Doe|john@example.com|Merge branches 'origin/feat-a' and 'origin/feat-b' into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat-b|4|3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|feat-b 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3|0|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat-b 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat-a|2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat-a commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|trunk commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 3
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".zoned())
            }
            result.branches["origin/feat-a"] shouldBe aBranch {
                name("origin/feat-a")
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                lastCommitHash("2".hash())
                lastCommitDate("2025-01-01T02:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".zoned(), "5")
            }
            result.branches["origin/feat-b"] shouldBe aBranch {
                name("origin/feat-b")
                firstCommitHash("3".hash())
                firstCommitDate("2025-01-01T03:00:00+01:00".zoned())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".zoned(), "5")
            }
        }

        @Test
        fun `should handle octopus merge with three parents and infer branch names from merge message`() {
            // trunk: 0───1───────────5 (HEAD -> trunk, origin/trunk) [octopus merge feat-a and feat-b]
            //         \    \        /
            // feat-a:  └─2──\──────/ (deleted, inferred from merge message)
            //                \    /
            // feat-b:         └─3-4 (deleted, inferred from merge message)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|5|1 2 4|2025-01-01T05:00:00+01:00|John Doe|john@example.com|Merge branches 'origin/feat-a' and 'origin/feat-b' into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |4|3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|feat-b 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3|0|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat-b 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat-a commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|trunk commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 3
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".zoned())
            }
            result.branches["origin/feat-a"] shouldBe aBranch {
                inferredName("origin/feat-a")
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                lastCommitHash("2".hash())
                lastCommitDate("2025-01-01T02:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".zoned(), "5")
            }
            result.branches["origin/feat-b"] shouldBe aBranch {
                inferredName("origin/feat-b")
                firstCommitHash("3".hash())
                firstCommitDate("2025-01-01T03:00:00+01:00".zoned())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".zoned(), "5")
            }
        }

        @Test
        fun `should handle octopus merge with three parents and unnamed branches`() {
            // trunk: 0───1───────────5 (HEAD -> trunk, origin/trunk) [octopus merge feat-a and feat-b]
            //         \    \        /
            // feat-a:  └─2──\──────/ (deleted, inferred from merge message)
            //                \    /
            // feat-b:         └─3-4 (deleted, inferred from merge message)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|5|1 2 4|2025-01-01T05:00:00+01:00|John Doe|john@example.com|Octopus merge all the things into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |4|3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|feat-b 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3|0|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat-b 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat-a commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|trunk commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 3
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".zoned())
            }
            result.branches.unnamed.shouldContainExactlyInAnyOrder(
                aBranch {
                    unNamed()
                    firstCommitHash("2".hash())
                    firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                    lastCommitHash("2".hash())
                    lastCommitDate("2025-01-01T02:00:00+01:00".zoned())
                    mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".zoned(), "5")
                },
                aBranch {
                    unNamed()
                    firstCommitHash("3".hash())
                    firstCommitDate("2025-01-01T03:00:00+01:00".zoned())
                    lastCommitHash("4".hash())
                    lastCommitDate("2025-01-01T04:00:00+01:00".zoned())
                    mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".zoned(), "5")
                }
            )
        }

        @Test
        fun `should handle octopus merge with three parents, one named, one unnamed`() {
            // trunk: 0───1───────────5 (HEAD -> trunk, origin/trunk) [octopus merge feat-a and feat-b]
            //         \    \        /
            // feat-a:  └─2──\──────/ (deleted, inferred from merge message)
            //                \    /
            // feat-b:         └─3-4 (origin/feat-b)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|5|1 2 4|2025-01-01T05:00:00+01:00|John Doe|john@example.com|Octopus merge all the things into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat-b|4|3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|feat-b 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3|0|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat-b 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat-a commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|trunk commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 3
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".zoned())
            }
            result.branches["origin/feat-b"] shouldBe aBranch {
                name("origin/feat-b")
                firstCommitHash("3".hash())
                firstCommitDate("2025-01-01T03:00:00+01:00".zoned())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".zoned(), "5")
            }
            result.branches.unnamed.shouldContainExactlyInAnyOrder(
                aBranch {
                    unNamed()
                    firstCommitHash("2".hash())
                    firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                    lastCommitHash("2".hash())
                    lastCommitDate("2025-01-01T02:00:00+01:00".zoned())
                    mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".zoned(), "5")
                }
            )
        }
    }

    @Nested
    inner class NestedBranches {

        @Test
        fun `should handle feature branch created from another feature branch`() {
            // trunk:   0───1─────────────────6 (origin/trunk) [merge feat-a]
            //           \                   /
            // feat-a:    └─2───3───────5───┘ (origin/feat-a) [merge feat-b]
            //                   \     /
            // feat-b:            └─4─┘ (origin/feat-b, branched from feat-a commit 3)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|6|1 5|2025-01-01T06:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat-a' into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat-a|5|3 4|2025-01-01T05:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat-b' into origin/feat-a
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat-b|4|3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|feat-b commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat-b.md

            -----COMMIT_START-----
            |3|2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat-a 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat-a2.md

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat-a 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat-a1.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|trunk commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk2.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 3
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash())
                lastCommitHash("6".hash())
                lastCommitDate("2025-01-01T06:00:00+01:00".zoned())
            }
            result.branches["origin/feat-a"] shouldBe aBranch {
                name("origin/feat-a")
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                intermediateCommits("3".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T06:00:00+01:00".zoned(), "6")
            }
            result.branches["origin/feat-b"] shouldBe aBranch {
                name("origin/feat-b")
                firstCommitHash("4".hash())
                firstCommitDate("2025-01-01T04:00:00+01:00".zoned())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".zoned())
                // TODO: mergeCommit info is NOT tracked because feat-b was merged into feat-a,
                // not into the active branch (trunk). The algorithm only tracks merges into the
                // active branch (commits with HEAD reference).
            }
        }
    }

    @Nested
    inner class MultipleMerges {

        @Test
        fun `should track only the last merge of the branch`() {
            // Feature branch is merged, then more work is done on it, then merged again
            // main:    0───1───────3───────5 (HEAD -> main, origin/main)
            //           \         /      /
            // feat:      └───────2───4───┘ (origin/feat)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|5|3 4|2025-01-01T05:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat' into origin/main
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat|4|2|2025-01-01T04:00:00+01:00|John Doe|john@example.com|feat commit 2
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat2.md

            -----COMMIT_START-----
            |3|1 2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat' into origin/main
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat commit 1
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat1.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       main.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       initial.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 2
            result.branches["origin/main"] shouldBe aBranch {
                name("origin/main")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash(), "3".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".zoned())
            }
            result.branches["origin/feat"] shouldBe aBranch {
                name("origin/feat")
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".zoned())
                // TODO multiple merges
                mergedInto("origin/main", "2025-01-01T05:00:00+01:00".zoned(), "5")
            }
        }

        @Test
        fun `should handle main merged twice into feature branch`() {
            // Main is merged into feature branch twice to keep it up to date
            // main:    0───1─────────4─────────7 (HEAD -> main, origin/main)
            //           \   \         \
            // feat:      └2──3─────5───6───8 (origin/feat)
            //                 ^         ^
            //            merge 1    merge 2

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            origin/feat|8|6|2025-01-01T08:00:00+01:00|John Doe|john@example.com|feat commit 5
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|7|4|2025-01-01T07:00:00+01:00|John Doe|john@example.com|main commit 3
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       main3.md

            -----COMMIT_START-----
            |6|5 4|2025-01-01T06:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/main' into origin/feat
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat3.md

            -----COMMIT_START-----
            |5|3|2025-01-01T05:00:00+01:00|John Doe|john@example.com|feat commit 3
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |4|1|2025-01-01T04:00:00+01:00|John Doe|john@example.com|main commit 2
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       main2.md

            -----COMMIT_START-----
            |3|2 1|2025-01-01T03:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/main' into origin/feat
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat commit 1
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat1.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit 1
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       main1.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       initial.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 2
            result.branches["origin/main"] shouldBe aBranch {
                name("origin/main")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash(), "4".hash())
                lastCommitHash("7".hash())
                lastCommitDate("2025-01-01T07:00:00+01:00".zoned())
            }
            result.branches["origin/feat"] shouldBe aBranch {
                name("origin/feat")
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                intermediateCommits("3".hash(), "5".hash(), "6".hash())
                lastCommitHash("8".hash())
                lastCommitDate("2025-01-01T08:00:00+01:00".zoned())
            }
        }

        @Test
        fun `should handle main merged twice into feature branch before merge into main`() {
            // Main is merged into feature branch twice to keep it up to date
            // main:    0───1─────────4───7 (HEAD -> main, origin/main)
            //           \   \         \ /
            // feat:      └2──3─────5───6 (origin/feat)
            //                 ^         ^
            //            merge 1    merge 2

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|7|4 6|2025-01-01T07:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat' into origin/main
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       main3.md

            -----COMMIT_START-----
            origin/feat|6|5 4|2025-01-01T06:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/main' into origin/feat
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat3.md

            -----COMMIT_START-----
            |5|3|2025-01-01T05:00:00+01:00|John Doe|john@example.com|feat commit 3
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |4|1|2025-01-01T04:00:00+01:00|John Doe|john@example.com|main commit 2
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       main2.md

            -----COMMIT_START-----
            |3|2 1|2025-01-01T03:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/main' into origin/feat
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat commit 1
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat1.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit 1
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       main1.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       initial.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 2
            result.branches["origin/main"] shouldBe aBranch {
                name("origin/main")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash(), "4".hash())
                lastCommitHash("7".hash())
                lastCommitDate("2025-01-01T07:00:00+01:00".zoned())
            }
            result.branches["origin/feat"] shouldBe aBranch {
                name("origin/feat")
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                intermediateCommits("5".hash(), "3".hash())
                lastCommitHash("6".hash())
                lastCommitDate("2025-01-01T06:00:00+01:00".zoned())
                mergedInto("origin/main", "2025-01-01T07:00:00+01:00".zoned(), "7")
            }
        }
    }

    @Nested
    inner class RealData {

        @Test
        fun `should extract branch info from merge commits with explicit merge commit`() {
            // trunk: 0───1───────4 (HEAD -> trunk, origin/trunk) [merge feat]
            //         \         /
            // feat:    └─2───3─┘ (origin/feat)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|4|1 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat' into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat|3|2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat2.md

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk2.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 2
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".zoned())
            }
            result.branches["origin/feat"] shouldBe aBranch {
                name("origin/feat")
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                lastCommitHash("3".hash())
                lastCommitDate("2025-01-01T03:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T04:00:00+01:00".zoned(), "4")
            }
        }

        @Test
        fun `should extract branch info from merge commits after branch is deleted and mark it as inferred`() {
            // trunk: 0───1───────4 (HEAD -> trunk, origin/trunk) [merge feat]
            //         \         /
            // feat:    └─2───3─┘ (deleted, inferred from merge message)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|4|1 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat' into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3|2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat2.md

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat.md

            -----COMMIT_START-----
            |1|0|2025-01-01T00:01:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk2.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 2
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".zoned())
            }
            result.branches["origin/feat"] shouldBe aBranch {
                inferredName("origin/feat")
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                lastCommitHash("3".hash())
                lastCommitDate("2025-01-01T03:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T04:00:00+01:00".zoned(), "4")
            }
        }

        @Test
        fun `should include branch as unnamed when branch is deleted and name cannot be inferred from merge message`() {
            // trunk: 0───1───────4 (HEAD -> trunk, origin/trunk) [merge with custom message]
            //         \         /
            // ???:     └─2───3─┘ (deleted, cannot infer name)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|4|1 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Squashed feature commits
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3|2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat2.md

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat.md

            -----COMMIT_START-----
            |1|0|2025-01-01T00:01:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk2.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 2
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".zoned())
            }
            result.branches.unnamed shouldHaveSize 1
            result.branches.unnamed.first() shouldBe aBranch {
                unNamed()
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                lastCommitHash("3".hash())
                lastCommitDate("2025-01-01T03:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T04:00:00+01:00".zoned(), "4")
            }
        }

        @Test
        fun `should extract branch info when main is merged into feat before feat is merged back`() {
            // trunk: 0───1───2───────────6 (origin/trunk) [merge feat]
            //         \       \         /
            // feat:    3───4───5───────┘ (origin/feat)
            //                   ^
            //                   merge trunk into feat (parents: 4, 2)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|6|2 5|2025-01-01T06:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat' into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat|5|4 2|2025-01-01T05:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/trunk' into origin/feat
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |4|3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|feat commit 2
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat2.md

            -----COMMIT_START-----
            |3|0|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat commit 1
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat1.md

            -----COMMIT_START-----
            |2|1|2025-01-01T02:00:00+01:00|John Doe|john@example.com|trunk commit 2
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk2.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|trunk commit 1
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk1.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 2
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash(), "2".hash())
                lastCommitHash("6".hash())
                lastCommitDate("2025-01-01T06:00:00+01:00".zoned())
            }
            result.branches["origin/feat"] shouldBe aBranch {
                name("origin/feat")
                firstCommitHash("3".hash())
                firstCommitDate("2025-01-01T03:00:00+01:00".zoned())
                intermediateCommits("4".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T06:00:00+01:00".zoned(), "6")
            }
        }

        @Test
        fun `should identify multiple inferred branches`() {
            // trunk: 0───1───────4───────7 (HEAD -> trunk, origin/trunk)
            //         \         / \     /
            // feat-a:  └─2───3─┘   \   / (deleted, inferred from merge message)
            //                       \ /
            // feat-b:                5─6 (deleted, inferred from merge message)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|7|4 6|2025-01-01T07:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat-b' into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |6|5|2025-01-01T06:00:00+01:00|John Doe|john@example.com|feat-b 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat-b2.md

            -----COMMIT_START-----
            |5|1|2025-01-01T05:00:00+01:00|John Doe|john@example.com|feat-b 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat-b1.md

            -----COMMIT_START-----
            |4|1 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feat-a' into origin/trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3|2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat-a 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat-a2.md

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat-a 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat-a1.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk2.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 3
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash(), "4".hash())
                lastCommitHash("7".hash())
                lastCommitDate("2025-01-01T07:00:00+01:00".zoned())
            }
            result.branches["origin/feat-a"] shouldBe aBranch {
                inferredName("origin/feat-a")
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                lastCommitHash("3".hash())
                lastCommitDate("2025-01-01T03:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T04:00:00+01:00".zoned(), "4")
            }
            result.branches["origin/feat-b"] shouldBe aBranch {
                inferredName("origin/feat-b")
                firstCommitHash("5".hash())
                firstCommitDate("2025-01-01T05:00:00+01:00".zoned())
                lastCommitHash("6".hash())
                lastCommitDate("2025-01-01T06:00:00+01:00".zoned())
                mergedInto("origin/trunk", "2025-01-01T07:00:00+01:00".zoned(), "7")
            }
        }

        @Test
        fun `should identify multiple unnamed branches`() {
            // trunk: 0───1───────4───────7 (HEAD -> trunk, origin/trunk)
            //         \         / \     /
            // ???:     └─2───3─┘   \   / (deleted, cannot infer name)
            //                       \ /
            // ???:                   5─6 (deleted, cannot infer name)

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|7|4 6|2025-01-01T07:00:00+01:00|John Doe|john@example.com|Squashed second feature
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |6|5|2025-01-01T06:00:00+01:00|John Doe|john@example.com|feat-b 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat-b2.md

            -----COMMIT_START-----
            |5|1|2025-01-01T05:00:00+01:00|John Doe|john@example.com|feat-b 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat-b1.md

            -----COMMIT_START-----
            |4|1 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Squashed first feature
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3|2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat-a 2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat-a2.md

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat-a 1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat-a1.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk2.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
        """.trimIndent()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
                intermediateCommits("1".hash(), "4".hash())
                lastCommitHash("7".hash())
                lastCommitDate("2025-01-01T07:00:00+01:00".zoned())
            }
            result.branches.unnamed.shouldContainExactlyInAnyOrder(
                aBranch {
                    unNamed()
                    firstCommitHash("2".hash())
                    firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                    lastCommitHash("3".hash())
                    lastCommitDate("2025-01-01T03:00:00+01:00".zoned())
                    mergedInto("origin/trunk", "2025-01-01T04:00:00+01:00".zoned(), "4")
                },
                aBranch {
                    unNamed()
                    firstCommitHash("5".hash())
                    firstCommitDate("2025-01-01T05:00:00+01:00".zoned())
                    lastCommitHash("6".hash())
                    lastCommitDate("2025-01-01T06:00:00+01:00".zoned())
                    mergedInto("origin/trunk", "2025-01-01T07:00:00+01:00".zoned(), "7")
                }
            )
            result.branches.size() shouldBe 3
        }
    }
}