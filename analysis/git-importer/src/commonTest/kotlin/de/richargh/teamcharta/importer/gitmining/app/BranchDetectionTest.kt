package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.gitfixtures.app.aGitLog
import de.richargh.teamcharta.importer.gitfixtures.app.api.hash
import de.richargh.teamcharta.importer.gitminingfixtures.app.api.aBranch
import de.richargh.teamcharta.importer.gitmining.app.api.BranchStatus
import de.richargh.teamcharta.importer.sharedfixtures.time.app.testNow2025
import de.richargh.teamcharta.importer.sharedfixtures.time.app.toInstant
import de.richargh.teamcharta.importer.sharedfixtures.time.app.atStartOfYear
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
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches["origin/main"] shouldBe aBranch {
                name("origin/main")
                isStale()
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
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

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
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches["origin/feature-login"] shouldBe aBranch {
                name("origin/feature-login")
                isCompleted()
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
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches["origin/feat"] shouldBe aBranch {
                name("origin/feat")
                isCompleted()
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
            val gitLogContent = aGitLog {
                isDeletedBranch("origin/feat")
                anEntry("origin/trunk") {
                    subject("initial commit")
                    authorDate(atStartOfYear(2021))
                    file("trunk.md")
                }
                anEntry("origin/trunk") {
                    subject("main commit")
                    authorDate(atStartOfYear(2022))
                    file("trunk2.md")
                }
                anEntry("origin/feat", "origin/trunk") {
                    subject("feat 1 commit")
                    authorDate(atStartOfYear(2023))
                    file("feat.md")
                }
                anEntry("origin/feat") {
                    subject("feat 2 commit")
                    authorDate(atStartOfYear(2024))
                    file("feat2.md")
                }
                anEntry("origin/trunk", "origin/feat") {
                    refHead("trunk")
                    subject("Merge branch 'origin/feat' into origin/trunk")
                    authorDate(atStartOfYear(2025))
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches["origin/feat"] shouldBe aBranch {
                inferredName("origin/feat")
                isCompleted()
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
            val gitLogContent = aGitLog {
                isDeletedBranch("unnamed-branch")
                anEntry("origin/trunk") {
                    subject("initial commit")
                    authorDate(atStartOfYear(2021))
                    file("trunk.md")
                }
                anEntry("origin/trunk") {
                    subject("main commit")
                    authorDate(atStartOfYear(2022))
                    file("trunk2.md")
                }
                anEntry("unnamed-branch", "origin/trunk") {
                    subject("feat 1 commit")
                    authorDate(atStartOfYear(2023))
                    file("feat.md")
                }
                anEntry("unnamed-branch") {
                    subject("feat 2 commit")
                    authorDate(atStartOfYear(2024))
                    file("feat2.md")
                }
                anEntry("origin/trunk", "unnamed-branch") {
                    refHead("trunk")
                    subject("Squashed feature commits")
                    authorDate(atStartOfYear(2025))
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches.unnamed shouldHaveSize 1
            result.branches.unnamed.first() shouldBe aBranch {
                unNamed("3")  // tipCommit = first commit seen (last commit of feature branch)
                isCompleted()
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
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.branches.size() shouldBe 1
        result.branches["origin/main"] shouldBe aBranch {
            name("origin/main")
            isStale()
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
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

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
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

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
            isCompleted()
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
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

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
            isCompleted()
            firstCommitHash("1".hash())
            firstCommitDate(atStartOfYear(2023))
            lastCommitHash("1".hash())
            lastCommitDate(atStartOfYear(2023))
            mergedInto("origin/main", atStartOfYear(2025), "3")
        }
        result.branches["origin/feature-user"] shouldBe aBranch {
            name("origin/feature-user")
            isStale()
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
        val gitLogContent = aGitLog {
            anEntry("origin/trunk") {
                authorDate("2025-01-01T00:00:00+01:00".toInstant())
            }
            anEntry("origin/feat", "origin/trunk") {
                authorDate("2025-01-01T01:00:00+01:00".toInstant())
            }
            anEntry("origin/feat") {
                authorDate("2025-01-01T02:00:00+01:00".toInstant())
            }
            anEntry("origin/feat") {
                authorDate("2025-01-01T03:00:00+01:00".toInstant())
            }
            anEntry("origin/feat") {
                authorDate("2025-01-01T04:00:00+01:00".toInstant())
            }
            anEntry("origin/feat") {
                authorDate("2025-01-01T05:00:00+01:00".toInstant())
            }
            anEntry("origin/trunk", "origin/feat") {
                refHead("trunk")
                authorDate("2025-01-01T06:00:00+01:00".toInstant())
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.branches.size() shouldBe 2
        result.branches["origin/feat"] shouldBe aBranch {
            name("origin/feat")
            isCompleted()
            firstCommitHash("1".hash())
            firstCommitDate("2025-01-01T01:00:00+01:00".toInstant())
            intermediateCommits("2".hash(), "3".hash(), "4".hash())
            lastCommitHash("5".hash())
            lastCommitDate("2025-01-01T05:00:00+01:00".toInstant())
            mergedInto("origin/trunk", "2025-01-01T06:00:00+01:00".toInstant(), "6")
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
            val gitLogContent = aGitLog {
                anEntry("origin/trunk") {
                    authorDate("2025-01-01T00:00:00+01:00".toInstant())
                }
                anEntry("origin/trunk") {
                    authorDate("2025-01-01T01:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-a", "origin/trunk") {
                    authorDate("2025-01-01T02:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-b", "origin/trunk") {
                    authorDate("2025-01-01T03:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-b") {
                    authorDate("2025-01-01T04:00:00+01:00".toInstant())
                }
                anEntry("origin/trunk", "origin/feat-b", "origin/feat-a") {
                    refHead("trunk")
                    authorDate("2025-01-01T05:00:00+01:00".toInstant())
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches.size() shouldBe 3
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                isCurrent()
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
                intermediateCommits("1".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".toInstant())
            }
            result.branches["origin/feat-b"] shouldBe aBranch {
                name("origin/feat-b")
                isCompleted()
                firstCommitHash("3".hash())
                firstCommitDate("2025-01-01T03:00:00+01:00".toInstant())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".toInstant())
                mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".toInstant(), "5")
            }
            result.branches["origin/feat-a"] shouldBe aBranch {
                name("origin/feat-a")
                isCompleted()
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                lastCommitHash("2".hash())
                lastCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".toInstant(), "5")
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
            val gitLogContent = aGitLog {
                isDeletedBranch("origin/feat-a")
                isDeletedBranch("origin/feat-b")
                anEntry("origin/trunk") {
                    authorDate("2025-01-01T00:00:00+01:00".toInstant())
                }
                anEntry("origin/trunk") {
                    authorDate("2025-01-01T01:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-a", "origin/trunk") {
                    authorDate("2025-01-01T02:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-b", "origin/trunk") {
                    authorDate("2025-01-01T03:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-b") {
                    authorDate("2025-01-01T04:00:00+01:00".toInstant())
                }
                anEntry("origin/trunk", "origin/feat-a", "origin/feat-b") {
                    refHead("trunk")
                    subject("Merge branches 'origin/feat-a' and 'origin/feat-b' into origin/trunk")
                    authorDate("2025-01-01T05:00:00+01:00".toInstant())
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches.size() shouldBe 3
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                isCurrent()
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
                intermediateCommits("1".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".toInstant())
            }
            result.branches["origin/feat-a"] shouldBe aBranch {
                inferredName("origin/feat-a")
                isCompleted()
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                lastCommitHash("2".hash())
                lastCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".toInstant(), "5")
            }
            result.branches["origin/feat-b"] shouldBe aBranch {
                inferredName("origin/feat-b")
                isCompleted()
                firstCommitHash("3".hash())
                firstCommitDate("2025-01-01T03:00:00+01:00".toInstant())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".toInstant())
                mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".toInstant(), "5")
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
            val gitLogContent = aGitLog {
                isDeletedBranch("origin/feat-a")
                isDeletedBranch("origin/feat-b")
                anEntry("origin/trunk") {
                    authorDate("2025-01-01T00:00:00+01:00".toInstant())
                }
                anEntry("origin/trunk") {
                    authorDate("2025-01-01T01:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-a", "origin/trunk") {
                    authorDate("2025-01-01T02:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-b", "origin/trunk") {
                    authorDate("2025-01-01T03:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-b") {
                    authorDate("2025-01-01T04:00:00+01:00".toInstant())
                }
                anEntry("origin/trunk", "origin/feat-b", "origin/feat-a") {
                    refHead("trunk")
                    subject("Octopus merge all the things into origin/trunk")
                    authorDate("2025-01-01T05:00:00+01:00".toInstant())
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches.size() shouldBe 3
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                isCurrent()
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
                intermediateCommits("1".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".toInstant())
            }
            result.branches.unnamed.shouldContainExactlyInAnyOrder(
                aBranch {
                    unNamed("2")  // tipCommit = first commit seen on this branch
                    isCompleted()
                    firstCommitHash("2".hash())
                    firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                    lastCommitHash("2".hash())
                    lastCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                    mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".toInstant(), "5")
                },
                aBranch {
                    unNamed("4")  // tipCommit = first commit seen (last commit of feature branch)
                    isCompleted()
                    firstCommitHash("3".hash())
                    firstCommitDate("2025-01-01T03:00:00+01:00".toInstant())
                    lastCommitHash("4".hash())
                    lastCommitDate("2025-01-01T04:00:00+01:00".toInstant())
                    mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".toInstant(), "5")
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
            val gitLogContent = aGitLog {
                isDeletedBranch("origin/feat-a")
                anEntry("origin/trunk") {
                    authorDate("2025-01-01T00:00:00+01:00".toInstant())
                }
                anEntry("origin/trunk") {
                    subject("trunk commit")
                    authorDate("2025-01-01T01:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-a", "origin/trunk") {
                    subject("feat-a commit")
                    authorDate("2025-01-01T02:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-b", "origin/trunk") {
                    subject("feat-b 1 commit")
                    authorDate("2025-01-01T03:00:00+01:00".toInstant())
                }
                anEntry("origin/feat-b") {
                    subject("feat-b 2 commit")
                    authorDate("2025-01-01T04:00:00+01:00".toInstant())
                }
                anEntry("origin/trunk", "origin/feat-b", "origin/feat-a") {
                    refHead("trunk")
                    subject("Octopus merge all the things into origin/trunk")
                    authorDate("2025-01-01T05:00:00+01:00".toInstant())
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches.size() shouldBe 3
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                isCurrent()
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
                intermediateCommits("1".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".toInstant())
            }
            result.branches["origin/feat-b"] shouldBe aBranch {
                name("origin/feat-b")
                isCompleted()
                firstCommitHash("3".hash())
                firstCommitDate("2025-01-01T03:00:00+01:00".toInstant())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".toInstant())
                mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".toInstant(), "5")
            }
            result.branches.unnamed.shouldContainExactlyInAnyOrder(
                aBranch {
                    unNamed("2")  // tipCommit = first commit seen on this branch
                    isCompleted()
                    firstCommitHash("2".hash())
                    firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                    lastCommitHash("2".hash())
                    lastCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                    mergedInto("origin/trunk", "2025-01-01T05:00:00+01:00".toInstant(), "5")
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
            val gitLogContent = aGitLog {
                anEntry("origin/trunk") {
                    subject("initial commit")
                    authorDate("2025-01-01T00:00:00+01:00".toInstant())
                    file("trunk.md")
                }
                anEntry("origin/trunk") {
                    subject("trunk commit")
                    authorDate("2025-01-01T01:00:00+01:00".toInstant())
                    file("trunk2.md")
                }
                anEntry("origin/feat-a", "origin/trunk") {
                    subject("feat-a 1 commit")
                    authorDate("2025-01-01T02:00:00+01:00".toInstant())
                    file("feat-a1.md")
                }
                anEntry("origin/feat-a") {
                    subject("feat-a 2 commit")
                    authorDate("2025-01-01T03:00:00+01:00".toInstant())
                    file("feat-a2.md")
                }
                anEntry("origin/feat-b", "origin/feat-a") {
                    subject("feat-b commit")
                    authorDate("2025-01-01T04:00:00+01:00".toInstant())
                    file("feat-b.md")
                }
                anEntry("origin/feat-a", "origin/feat-b") {
                    subject("Merge branch 'origin/feat-b' into origin/feat-a")
                    authorDate("2025-01-01T05:00:00+01:00".toInstant())
                }
                anEntry("origin/trunk", "origin/feat-a") {
                    refHead("trunk")
                    subject("Merge branch 'origin/feat-a' into origin/trunk")
                    authorDate("2025-01-01T06:00:00+01:00".toInstant())
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches.size() shouldBe 3
            result.branches["origin/trunk"] shouldBe aBranch {
                name("origin/trunk")
                isCurrent()
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
                intermediateCommits("1".hash())
                lastCommitHash("6".hash())
                lastCommitDate("2025-01-01T06:00:00+01:00".toInstant())
            }
            result.branches["origin/feat-a"] shouldBe aBranch {
                name("origin/feat-a")
                isCompleted()
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                intermediateCommits("3".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".toInstant())
                mergedInto("origin/trunk", "2025-01-01T06:00:00+01:00".toInstant(), "6")
            }
            result.branches["origin/feat-b"] shouldBe aBranch {
                name("origin/feat-b")
                isCompleted()
                firstCommitHash("4".hash())
                firstCommitDate("2025-01-01T04:00:00+01:00".toInstant())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".toInstant())
                mergedInto("origin/feat-a", "2025-01-01T05:00:00+01:00".toInstant(), "5")
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
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    subject("initial commit")
                    authorDate("2025-01-01T00:00:00+01:00".toInstant())
                    file("initial.md")
                }
                anEntry("origin/main") {
                    subject("main commit")
                    authorDate("2025-01-01T01:00:00+01:00".toInstant())
                    file("main.md")
                }
                anEntry("origin/feat", "origin/main") {
                    subject("feat commit 1")
                    authorDate("2025-01-01T02:00:00+01:00".toInstant())
                    file("feat1.md")
                }
                anEntry("origin/main", "origin/feat") {
                    subject("Merge branch 'origin/feat' into origin/main")
                    authorDate("2025-01-01T03:00:00+01:00".toInstant())
                }
                anEntry("origin/feat") {
                    subject("feat commit 2")
                    authorDate("2025-01-01T04:00:00+01:00".toInstant())
                    file("feat2.md")
                }
                anEntry("origin/main", "origin/feat") {
                    refHead("main")
                    subject("Merge branch 'origin/feat' into origin/main")
                    authorDate("2025-01-01T05:00:00+01:00".toInstant())
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches.size() shouldBe 2
            result.branches["origin/main"] shouldBe aBranch {
                name("origin/main")
                isCurrent()
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
                intermediateCommits("1".hash(), "3".hash())
                lastCommitHash("5".hash())
                lastCommitDate("2025-01-01T05:00:00+01:00".toInstant())
            }
            result.branches["origin/feat"] shouldBe aBranch {
                name("origin/feat")
                isCompleted()
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                lastCommitHash("4".hash())
                lastCommitDate("2025-01-01T04:00:00+01:00".toInstant())
                // TODO multiple merges
                mergedInto("origin/main", "2025-01-01T05:00:00+01:00".toInstant(), "5")
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
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    subject("initial commit")
                    authorDate("2025-01-01T00:00:00+01:00".toInstant())
                    file("initial.md")
                }
                anEntry("origin/main") {
                    subject("main commit 1")
                    authorDate("2025-01-01T01:00:00+01:00".toInstant())
                    file("main1.md")
                }
                anEntry("origin/feat", "origin/main") {
                    subject("feat commit 1")
                    authorDate("2025-01-01T02:00:00+01:00".toInstant())
                    file("feat1.md")
                }
                anEntry("origin/feat", "origin/main") {
                    subject("Merge branch 'origin/main' into origin/feat")
                    authorDate("2025-01-01T03:00:00+01:00".toInstant())
                }
                anEntry("origin/main") {
                    subject("main commit 2")
                    authorDate("2025-01-01T04:00:00+01:00".toInstant())
                    file("main2.md")
                }
                anEntry("origin/feat") {
                    subject("feat commit 3")
                    authorDate("2025-01-01T05:00:00+01:00".toInstant())
                }
                anEntry("origin/feat", "origin/main") {
                    subject("Merge branch 'origin/main' into origin/feat")
                    authorDate("2025-01-01T06:00:00+01:00".toInstant())
                    file("feat3.md")
                }
                anEntry("origin/main") {
                    refHead("main")
                    subject("main commit 3")
                    authorDate("2025-01-01T07:00:00+01:00".toInstant())
                    file("main3.md")
                }
                anEntry("origin/feat") {
                    subject("feat commit 5")
                    authorDate("2025-01-01T08:00:00+01:00".toInstant())
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches.size() shouldBe 2
            result.branches["origin/main"] shouldBe aBranch {
                name("origin/main")
                isCurrent()
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
                intermediateCommits("1".hash(), "4".hash())
                lastCommitHash("7".hash())
                lastCommitDate("2025-01-01T07:00:00+01:00".toInstant())
            }
            result.branches["origin/feat"] shouldBe aBranch {
                name("origin/feat")
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                intermediateCommits("3".hash(), "5".hash(), "6".hash())
                lastCommitHash("8".hash())
                lastCommitDate("2025-01-01T08:00:00+01:00".toInstant())
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
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    subject("initial commit")
                    authorDate("2025-01-01T00:00:00+01:00".toInstant())
                    file("initial.md")
                }
                anEntry("origin/main") {
                    subject("main commit 1")
                    authorDate("2025-01-01T01:00:00+01:00".toInstant())
                    file("main1.md")
                }
                anEntry("origin/feat", "origin/main") {
                    subject("feat commit 1")
                    authorDate("2025-01-01T02:00:00+01:00".toInstant())
                    file("feat1.md")
                }
                anEntry("origin/feat", "origin/main") {
                    subject("Merge branch 'origin/main' into origin/feat")
                    authorDate("2025-01-01T03:00:00+01:00".toInstant())
                }
                anEntry("origin/main") {
                    subject("main commit 2")
                    authorDate("2025-01-01T04:00:00+01:00".toInstant())
                    file("main2.md")
                }
                anEntry("origin/feat") {
                    subject("feat commit 3")
                    authorDate("2025-01-01T05:00:00+01:00".toInstant())
                }
                anEntry("origin/feat", "origin/main") {
                    subject("Merge branch 'origin/main' into origin/feat")
                    authorDate("2025-01-01T06:00:00+01:00".toInstant())
                    file("feat3.md")
                }
                anEntry("origin/main", "origin/feat") {
                    refHead("main")
                    subject("Merge branch 'origin/feat' into origin/main")
                    authorDate("2025-01-01T07:00:00+01:00".toInstant())
                    file("main3.md")
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches.size() shouldBe 2
            result.branches["origin/main"] shouldBe aBranch {
                name("origin/main")
                isCurrent()
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
                intermediateCommits("1".hash(), "4".hash())
                lastCommitHash("7".hash())
                lastCommitDate("2025-01-01T07:00:00+01:00".toInstant())
            }
            result.branches["origin/feat"] shouldBe aBranch {
                name("origin/feat")
                isCompleted()
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                intermediateCommits("5".hash(), "3".hash())
                lastCommitHash("6".hash())
                lastCommitDate("2025-01-01T06:00:00+01:00".toInstant())
                mergedInto("origin/main", "2025-01-01T07:00:00+01:00".toInstant(), "7")
            }
        }
    }


    @Test
    fun `should extract branch info from merge commits with explicit merge commit`() {
        // trunk: 0───1───────4 (HEAD -> trunk, origin/trunk) [merge feat]
        //         \         /
        // feat:    └─2───3─┘ (origin/feat)

        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/trunk") {
                subject("initial commit")
                authorDate("2025-01-01T00:00:00+01:00".toInstant())
                file("trunk.md")
            }
            anEntry("origin/trunk") {
                subject("main commit")
                authorDate("2025-01-01T01:00:00+01:00".toInstant())
                file("trunk2.md")
            }
            anEntry("origin/feat", "origin/trunk") {
                subject("feat 1 commit")
                authorDate("2025-01-01T02:00:00+01:00".toInstant())
                file("feat.md")
            }
            anEntry("origin/feat") {
                subject("feat 2 commit")
                authorDate("2025-01-01T03:00:00+01:00".toInstant())
                file("feat2.md")
            }
            anEntry("origin/trunk", "origin/feat") {
                refHead("trunk")
                subject("Merge branch 'origin/feat' into origin/trunk")
                authorDate("2025-01-01T04:00:00+01:00".toInstant())
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.branches.size() shouldBe 2
        result.branches["origin/trunk"] shouldBe aBranch {
            name("origin/trunk")
            isCurrent()
            firstCommitHash("0".hash())
            firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
            intermediateCommits("1".hash())
            lastCommitHash("4".hash())
            lastCommitDate("2025-01-01T04:00:00+01:00".toInstant())
        }
        result.branches["origin/feat"] shouldBe aBranch {
            name("origin/feat")
            isCompleted()
            firstCommitHash("2".hash())
            firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
            lastCommitHash("3".hash())
            lastCommitDate("2025-01-01T03:00:00+01:00".toInstant())
            mergedInto("origin/trunk", "2025-01-01T04:00:00+01:00".toInstant(), "4")
        }
    }

    @Test
    fun `should extract branch info from merge commits after branch is deleted and mark it as inferred`() {
        // trunk: 0───1───────4 (HEAD -> trunk, origin/trunk) [merge feat]
        //         \         /
        // feat:    └─2───3─┘ (deleted, inferred from merge message)

        // Given
        val gitLogContent = aGitLog {
            isDeletedBranch("origin/feat")
            anEntry("origin/trunk") {
                subject("initial commit")
                authorDate("2025-01-01T00:00:00+01:00".toInstant())
                file("trunk.md")
            }
            anEntry("origin/trunk") {
                subject("main commit")
                authorDate("2025-01-01T01:00:00+01:00".toInstant())
                file("trunk2.md")
            }
            anEntry("origin/feat", "origin/trunk") {
                subject("feat 1 commit")
                authorDate("2025-01-01T02:00:00+01:00".toInstant())
                file("feat.md")
            }
            anEntry("origin/feat") {
                subject("feat 2 commit")
                authorDate("2025-01-01T03:00:00+01:00".toInstant())
                file("feat2.md")
            }
            anEntry("origin/trunk", "origin/feat") {
                refHead("trunk")
                subject("Merge branch 'origin/feat' into origin/trunk")
                authorDate("2025-01-01T04:00:00+01:00".toInstant())
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.branches.size() shouldBe 2
        result.branches["origin/trunk"] shouldBe aBranch {
            name("origin/trunk")
            isCurrent()
            firstCommitHash("0".hash())
            firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
            intermediateCommits("1".hash())
            lastCommitHash("4".hash())
            lastCommitDate("2025-01-01T04:00:00+01:00".toInstant())
        }
        result.branches["origin/feat"] shouldBe aBranch {
            inferredName("origin/feat")
            isCompleted()
            firstCommitHash("2".hash())
            firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
            lastCommitHash("3".hash())
            lastCommitDate("2025-01-01T03:00:00+01:00".toInstant())
            mergedInto("origin/trunk", "2025-01-01T04:00:00+01:00".toInstant(), "4")
        }
    }

    @Test
    fun `should include branch as unnamed when branch is deleted and name cannot be inferred from merge message`() {
        // trunk: 0───1───────4 (HEAD -> trunk, origin/trunk) [merge with custom message]
        //         \         /
        // ???:     └─2───3─┘ (deleted, cannot infer name)

        // Given
        val gitLogContent = aGitLog {
            isDeletedBranch("unnamed-branch")
            anEntry("origin/trunk") {
                subject("initial commit")
                authorDate("2025-01-01T00:00:00+01:00".toInstant())
                file("trunk.md")
            }
            anEntry("origin/trunk") {
                subject("main commit")
                authorDate("2025-01-01T00:01:00+01:00".toInstant())
                file("trunk2.md")
            }
            anEntry("unnamed-branch", "origin/trunk") {
                subject("feat 1 commit")
                authorDate("2025-01-01T02:00:00+01:00".toInstant())
                file("feat.md")
            }
            anEntry("unnamed-branch") {
                subject("feat 2 commit")
                authorDate("2025-01-01T03:00:00+01:00".toInstant())
                file("feat2.md")
            }
            anEntry("origin/trunk", "unnamed-branch") {
                refHead("trunk")
                subject("Squashed feature commits")
                authorDate("2025-01-01T04:00:00+01:00".toInstant())
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.branches.size() shouldBe 2
        result.branches["origin/trunk"] shouldBe aBranch {
            name("origin/trunk")
            isCurrent()
            firstCommitHash("0".hash())
            firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
            intermediateCommits("1".hash())
            lastCommitHash("4".hash())
            lastCommitDate("2025-01-01T04:00:00+01:00".toInstant())
        }
        result.branches.unnamed shouldHaveSize 1
        result.branches.unnamed.first() shouldBe aBranch {
            unNamed("3")  // tipCommit = first commit seen (last commit of feature branch)
            isCompleted()
            firstCommitHash("2".hash())
            firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
            lastCommitHash("3".hash())
            lastCommitDate("2025-01-01T03:00:00+01:00".toInstant())
            mergedInto("origin/trunk", "2025-01-01T04:00:00+01:00".toInstant(), "4")
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
        val gitLogContent = aGitLog {
            anEntry("origin/trunk") {
                subject("initial commit")
                authorDate("2025-01-01T00:00:00+01:00".toInstant())
                file("trunk.md")
            }
            anEntry("origin/trunk") {
                subject("trunk commit 1")
                authorDate("2025-01-01T01:00:00+01:00".toInstant())
                file("trunk1.md")
            }
            anEntry("origin/trunk") {
                subject("trunk commit 2")
                authorDate("2025-01-01T02:00:00+01:00".toInstant())
                file("trunk2.md")
            }
            anEntry("origin/feat", "origin/trunk") {
                subject("feat commit 1")
                authorDate("2025-01-01T03:00:00+01:00".toInstant())
                file("feat1.md")
            }
            anEntry("origin/feat") {
                subject("feat commit 2")
                authorDate("2025-01-01T04:00:00+01:00".toInstant())
                file("feat2.md")
            }
            anEntry("origin/feat", "origin/trunk") {
                subject("Merge branch 'origin/trunk' into origin/feat")
                authorDate("2025-01-01T05:00:00+01:00".toInstant())
            }
            anEntry("origin/trunk", "origin/feat") {
                refHead("trunk")
                subject("Merge branch 'origin/feat' into origin/trunk")
                authorDate("2025-01-01T06:00:00+01:00".toInstant())
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.branches.size() shouldBe 2
        result.branches["origin/trunk"] shouldBe aBranch {
            name("origin/trunk")
            isCurrent()
            firstCommitHash("0".hash())
            firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
            intermediateCommits("1".hash(), "2".hash())
            lastCommitHash("6".hash())
            lastCommitDate("2025-01-01T06:00:00+01:00".toInstant())
        }
        result.branches["origin/feat"] shouldBe aBranch {
            name("origin/feat")
            isCompleted()
            firstCommitHash("3".hash())
            firstCommitDate("2025-01-01T03:00:00+01:00".toInstant())
            intermediateCommits("4".hash())
            lastCommitHash("5".hash())
            lastCommitDate("2025-01-01T05:00:00+01:00".toInstant())
            mergedInto("origin/trunk", "2025-01-01T06:00:00+01:00".toInstant(), "6")
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
        val gitLogContent = aGitLog {
            isDeletedBranch("origin/feat-a")
            isDeletedBranch("origin/feat-b")
            anEntry("origin/trunk") {
                subject("initial commit")
                authorDate("2025-01-01T00:00:00+01:00".toInstant())
                file("trunk.md")
            }
            anEntry("origin/trunk") {
                subject("main commit")
                authorDate("2025-01-01T01:00:00+01:00".toInstant())
                file("trunk2.md")
            }
            anEntry("origin/feat-a", "origin/trunk") {
                subject("feat-a 1 commit")
                authorDate("2025-01-01T02:00:00+01:00".toInstant())
                file("feat-a1.md")
            }
            anEntry("origin/feat-a") {
                subject("feat-a 2 commit")
                authorDate("2025-01-01T03:00:00+01:00".toInstant())
                file("feat-a2.md")
            }
            anEntry("origin/trunk", "origin/feat-a") {
                subject("Merge branch 'origin/feat-a' into origin/trunk")
                authorDate("2025-01-01T04:00:00+01:00".toInstant())
            }
            anEntry("origin/feat-b", "origin/trunk") {
                subject("feat-b 1 commit")
                authorDate("2025-01-01T05:00:00+01:00".toInstant())
                file("feat-b1.md")
            }
            anEntry("origin/feat-b") {
                subject("feat-b 2 commit")
                authorDate("2025-01-01T06:00:00+01:00".toInstant())
                file("feat-b2.md")
            }
            anEntry("origin/trunk", "origin/feat-b") {
                refHead("trunk")
                subject("Merge branch 'origin/feat-b' into origin/trunk")
                authorDate("2025-01-01T07:00:00+01:00".toInstant())
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.branches.size() shouldBe 3
        result.branches["origin/trunk"] shouldBe aBranch {
            name("origin/trunk")
            isCurrent()
            firstCommitHash("0".hash())
            firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
            intermediateCommits("1".hash(), "4".hash())
            lastCommitHash("7".hash())
            lastCommitDate("2025-01-01T07:00:00+01:00".toInstant())
        }
        result.branches["origin/feat-a"] shouldBe aBranch {
            inferredName("origin/feat-a")
            isCompleted()
            firstCommitHash("2".hash())
            firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
            lastCommitHash("3".hash())
            lastCommitDate("2025-01-01T03:00:00+01:00".toInstant())
            mergedInto("origin/trunk", "2025-01-01T04:00:00+01:00".toInstant(), "4")
        }
        result.branches["origin/feat-b"] shouldBe aBranch {
            inferredName("origin/feat-b")
            isCompleted()
            firstCommitHash("5".hash())
            firstCommitDate("2025-01-01T05:00:00+01:00".toInstant())
            lastCommitHash("6".hash())
            lastCommitDate("2025-01-01T06:00:00+01:00".toInstant())
            mergedInto("origin/trunk", "2025-01-01T07:00:00+01:00".toInstant(), "7")
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
        val gitLogContent = aGitLog {
            isDeletedBranch("unnamed-branch-a")
            isDeletedBranch("unnamed-branch-b")
            anEntry("origin/trunk") {
                subject("initial commit")
                authorDate("2025-01-01T00:00:00+01:00".toInstant())
                file("trunk.md")
            }
            anEntry("origin/trunk") {
                subject("main commit")
                authorDate("2025-01-01T01:00:00+01:00".toInstant())
                file("trunk2.md")
            }
            anEntry("unnamed-branch-a", "origin/trunk") {
                subject("feat-a 1 commit")
                authorDate("2025-01-01T02:00:00+01:00".toInstant())
                file("feat-a1.md")
            }
            anEntry("unnamed-branch-a") {
                subject("feat-a 2 commit")
                authorDate("2025-01-01T03:00:00+01:00".toInstant())
                file("feat-a2.md")
            }
            anEntry("origin/trunk", "unnamed-branch-a") {
                subject("Squashed first feature")
                authorDate("2025-01-01T04:00:00+01:00".toInstant())
            }
            anEntry("unnamed-branch-b", "origin/trunk") {
                subject("feat-b 1 commit")
                authorDate("2025-01-01T05:00:00+01:00".toInstant())
                file("feat-b1.md")
            }
            anEntry("unnamed-branch-b") {
                subject("feat-b 2 commit")
                authorDate("2025-01-01T06:00:00+01:00".toInstant())
                file("feat-b2.md")
            }
            anEntry("origin/trunk", "unnamed-branch-b") {
                refHead("trunk")
                subject("Squashed second feature")
                authorDate("2025-01-01T07:00:00+01:00".toInstant())
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.branches["origin/trunk"] shouldBe aBranch {
            name("origin/trunk")
            isCurrent()
            firstCommitHash("0".hash())
            firstCommitDate("2025-01-01T00:00:00+01:00".toInstant())
            intermediateCommits("1".hash(), "4".hash())
            lastCommitHash("7".hash())
            lastCommitDate("2025-01-01T07:00:00+01:00".toInstant())
        }
        result.branches.unnamed.shouldContainExactlyInAnyOrder(
            aBranch {
                unNamed("3")  // tipCommit = first commit seen (last commit of feature branch)
                isCompleted()
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".toInstant())
                lastCommitHash("3".hash())
                lastCommitDate("2025-01-01T03:00:00+01:00".toInstant())
                mergedInto("origin/trunk", "2025-01-01T04:00:00+01:00".toInstant(), "4")
            },
            aBranch {
                unNamed("6")  // tipCommit = first commit seen (last commit of feature branch)
                isCompleted()
                firstCommitHash("5".hash())
                firstCommitDate("2025-01-01T05:00:00+01:00".toInstant())
                lastCommitHash("6".hash())
                lastCommitDate("2025-01-01T06:00:00+01:00".toInstant())
                mergedInto("origin/trunk", "2025-01-01T07:00:00+01:00".toInstant(), "7")
            }
        )
        result.branches.size() shouldBe 3
    }

    @Nested
    inner class BranchCompletion {

        @Test
        fun `should mark branch as completed when merged with no additional commits`() {
            // main:    0───2 (origin/main) [merge feature]
            //           \ /
            // feature:   1 (origin/feature)

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2023))
                }
                anEntry("origin/feature", "origin/main") {
                    authorDate(atStartOfYear(2024))
                }
                anEntry("origin/main", "origin/feature") {
                    authorDate(atStartOfYear(2025))
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches["origin/feature"]!!.status shouldBe BranchStatus.Completed
        }

        @Test
        fun `should mark branch as not completed when it has commits after merge`() {
            // Feature branch is merged, then more work is done on it
            // main:    0───1───────3 (HEAD -> main, origin/main) [merge feat]
            //           \         /
            // feat:      └───────2───4 (origin/feat)
            //                        ^ commit after merge

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    subject("initial commit")
                    authorDate("2025-01-01T00:00:00+01:00".toInstant())
                    file("initial.md")
                }
                anEntry("origin/main") {
                    subject("main commit")
                    authorDate("2025-01-01T01:00:00+01:00".toInstant())
                    file("main.md")
                }
                anEntry("origin/feat", "origin/main") {
                    subject("feat commit 1")
                    authorDate("2025-01-01T02:00:00+01:00".toInstant())
                    file("feat1.md")
                }
                anEntry("origin/main", "origin/feat") {
                    refHead("main")
                    subject("Merge branch 'origin/feat' into origin/main")
                    authorDate("2025-01-01T03:00:00+01:00".toInstant())
                }
                anEntry("origin/feat") {
                    subject("feat commit after merge")
                    authorDate("2025-01-01T04:00:00+01:00".toInstant())
                    file("feat2.md")
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches["origin/feat"]!!.status shouldBe BranchStatus.Active
        }

        @Test
        fun `should mark branch as not completed when never merged`() {
            // main:    0───1 (origin/main)
            //           \
            // feature:   2 (origin/feature) - never merged

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2023))
                }
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2024))
                }
                anEntry("origin/feature", "origin/main") {
                    authorDate(atStartOfYear(2025))
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches["origin/feature"]!!.status shouldBe BranchStatus.Active
        }

        @Test
        fun `should mark main as not completed at all times, here when a feature was merged into main`() {
            // main:    0───2 (origin/main) [merge feature]
            //           \ /
            // feature:   1 (origin/feature)

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2023))
                }
                anEntry("origin/feature", "origin/main") {
                    authorDate(atStartOfYear(2024))
                }
                anEntry("origin/main", "origin/feature") {
                    authorDate(atStartOfYear(2025))
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches["origin/main"]!!.status shouldBe BranchStatus.Active
        }

        @Test
        fun `should mark main as not completed at all times, here when feature branch is created`() {
            // main:    0 (origin/main)
            //           \
            // feature:   1 (origin/feature) - never merged

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2024))
                }
                anEntry("origin/feature", "origin/main") {
                    authorDate(atStartOfYear(2025))
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches["origin/main"]!!.status shouldBe BranchStatus.Stale
        }

        @Test
        fun `should mark branch as not completed when commit after merge has backdated date`() {
            // Feature branch is merged, then a commit is added with a date BEFORE the merge
            // This tests that we use commit graph topology, not dates, to detect post-merge commits
            // main:    0───1───────3 (HEAD -> main, origin/main) [merge feat]
            //           \         /
            // feat:      └───────2───4 (origin/feat)
            //                        ^ commit after merge with backdated date

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    subject("initial commit")
                    authorDate("2022-01-01T00:00:00+01:00".toInstant())
                    file("initial.md")
                }
                anEntry("origin/main") {
                    subject("main commit")
                    authorDate("2023-01-01T01:00:00+01:00".toInstant())
                    file("main.md")
                }
                anEntry("origin/feat", "origin/main") {
                    subject("feat commit 1")
                    authorDate("2024-01-01T02:00:00+01:00".toInstant())
                    file("feat1.md")
                }
                anEntry("origin/main", "origin/feat") {
                    refHead("main")
                    subject("Merge branch 'origin/feat' into origin/main")
                    authorDate("2025-01-01T03:00:00+01:00".toInstant())
                }
                anEntry("origin/feat") {
                    subject("feat commit after merge with old date")
                    authorDate("2022-01-01T00:00:00+01:00".toInstant())
                    file("feat2.md")
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches["origin/feat"]!!.status shouldBe BranchStatus.Stale
        }

        @Test
        fun `should mark main as not completed at all times, here when merged into a feature branch`() {
            // main:    0───2 (origin/main)
            //           \   \
            // feature:   1───3 (origin/feature) - never merged

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2022))
                }
                anEntry("origin/feature", "origin/main") {
                    authorDate(atStartOfYear(2023))
                }
                anEntry("origin/main") {
                    authorDate(atStartOfYear(2024))
                }
                anEntry("origin/main", "origin/feature") {
                    authorDate(atStartOfYear(2025))
                }
            }

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

            // Then
            result.branches["origin/main"]!!.status shouldBe BranchStatus.Active
        }
    }

    @Nested
    inner class BranchActivity {

        @Test
        fun `should mark branch as active when last commit is within 3 months of current date`() {
            // main: 0 (origin/main)

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate("2025-01-15T10:00:00+01:00".toInstant())
                }
            }
            val currentDate = "2025-03-01T00:00:00+01:00".toInstant()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), currentDate)

            // Then
            result.branches["origin/main"]!!.status shouldBe BranchStatus.Active
        }

        @Test
        fun `should mark branch as stale when last commit is older than 3 months`() {
            // main: 0 (origin/main)

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate("2024-01-15T10:00:00+01:00".toInstant())
                }
            }
            val currentDate = "2025-06-01T00:00:00+01:00".toInstant()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), currentDate)

            // Then
            result.branches["origin/main"]!!.status shouldBe BranchStatus.Stale
        }

        @Test
        fun `should mark branch as active when last commit is exactly 3 months ago`() {
            // main: 0 (origin/main)

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate("2025-01-01T00:00:00+01:00".toInstant())
                }
            }
            val currentDate = "2025-04-01T00:00:00+01:00".toInstant()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), currentDate)

            // Then
            result.branches["origin/main"]!!.status shouldBe BranchStatus.Active
        }

        @Test
        fun `should mark branch as stale when last commit is just over 3 months ago`() {
            // main: 0 (origin/main)

            // Given
            val gitLogContent = aGitLog {
                anEntry("origin/main") {
                    authorDate("2024-12-31T23:59:59+01:00".toInstant())
                }
            }
            val currentDate = "2025-04-01T00:00:00+01:00".toInstant()

            val testee = GitLogMiner()

            // When
            val result = testee.parse(gitLogContent.lineSequence(), currentDate)

            // Then
            result.branches["origin/main"]!!.status shouldBe BranchStatus.Stale
        }
    }
}