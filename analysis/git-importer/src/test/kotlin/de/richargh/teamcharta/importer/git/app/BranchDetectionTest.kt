package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.aBranch
import de.richargh.teamcharta.importer.git.app.api2.hash
import de.richargh.teamcharta.importer.git.app.internal.atStartOfYear
import de.richargh.teamcharta.importer.git.app.internal.zoned
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BranchDetectionTest {

    @Test
    fun `should extract branch info for initial commit`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                authorDate(atStartOfYear(2024))
            }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 1
        result.branches["origin/main"] shouldBe aBranch {
            name("origin/main")
            firstCommitHash("0".hash())
            firstCommitDate(atStartOfYear(2024))
        }
    }

    @Test
    fun `should extract branch info for multiple commits on branch`() {
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

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 1
        result.branches["origin/main"] shouldBe aBranch {
            name("origin/main")
            firstCommitHash("0".hash())
            firstCommitDate(atStartOfYear(2023))
        }
    }

    @Test
    fun `should extract branch info from merge commits`() {
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

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 2
        result.branches["origin/main"] shouldBe aBranch {
            name("origin/main")
            firstCommitHash("0".hash())
            firstCommitDate(atStartOfYear(2023))
        }
        result.branches["origin/feature-login"] shouldBe aBranch {
            name("origin/feature-login")
            firstCommitHash("1".hash())
            firstCommitDate(atStartOfYear(2024))
            mergedInto("origin/main", atStartOfYear(2025), "2")
        }
    }

    @Test
    fun `should extract branch info when one branch is unmerged`() {
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

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 3
        result.branches["origin/main"] shouldBe aBranch {
            name("origin/main")
            firstCommitHash("0".hash())
            firstCommitDate(atStartOfYear(2022))
        }
        result.branches["origin/feature-login"] shouldBe aBranch {
            name("origin/feature-login")
            firstCommitHash("1".hash())
            firstCommitDate(atStartOfYear(2023))
            mergedInto("origin/main", atStartOfYear(2025), "3")
        }
        result.branches["origin/feature-user"] shouldBe aBranch {
            name("origin/feature-user")
            firstCommitHash("2".hash())
            firstCommitDate(atStartOfYear(2024))
        }
    }

    @Nested
    inner class RealData {

        @Test
        fun `should extract branch info from merge commits with explicit merge commit`() {
            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            hash==>> 0
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2025-01-01T00:00:00+01:00
            subject==>> initial commit
            parents==>>
            refs==>>
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
            
            -----COMMIT_START-----
            hash==>> 1
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2025-01-01T01:00:00+01:00
            subject==>> main commit
            parents==>> 0
            refs==>>
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk2.md

            -----COMMIT_START-----
            hash==>> 2
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2025-01-01T02:00:00+01:00
            subject==>> feat 1 commit
            parents==>> 0
            refs==>>
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat.md

            -----COMMIT_START-----
            hash==>> 3
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2025-01-01T03:00:00+01:00
            subject==>> feat 2 commit
            parents==>> 2
            refs==>> feat
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat2.md

            -----COMMIT_START-----
            hash==>> 4
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2025-01-01T04:00:00+01:00
            subject==>> Merge branch 'feat' into trunk
            parents==>> 1 3
            refs==>> HEAD -> trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

            val testee = GitLogParser2()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 2
            result.branches["trunk"] shouldBe aBranch {
                name("trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
            }
            result.branches["feat"] shouldBe aBranch {
                name("feat")
                firstCommitHash("2".hash())
                firstCommitDate("2025-01-01T02:00:00+01:00".zoned())
                mergedInto("trunk", "2025-01-01T04:00:00+01:00".zoned(), "4")
            }
        }

        @Test
        fun `should extract branch info from merge commits after branch is deleted`() {
            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            hash==>> 0
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2025-01-01T00:00:00+01:00
            subject==>> initial commit
            parents==>>
            refs==>>
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk.md
            
            -----COMMIT_START-----
            hash==>> 1
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2025-01-01T00:01:00+01:00
            subject==>> main commit
            parents==>> 0
            refs==>>
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       trunk2.md
            
            -----COMMIT_START-----
            hash==>> 2
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2025-01-01T02:00:00+01:00
            subject==>> feat 1 commit
            parents==>> 0
            refs==>>
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat.md
            
            -----COMMIT_START-----
            hash==>> 3
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2025-01-01T03:00:00+01:00
            subject==>> feat 2 commit
            parents==>> 2
            refs==>>
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0       0       feat2.md
            
            -----COMMIT_START-----
            hash==>> 4
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2025-01-01T04:00:00+01:00
            subject==>> Merge branch 'feat' into trunk
            parents==>> 1 3
            refs==>> HEAD -> trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

            val testee = GitLogParser2()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.branches.size() shouldBe 1
            result.branches["trunk"] shouldBe aBranch {
                name("trunk")
                firstCommitHash("0".hash())
                firstCommitDate("2025-01-01T00:00:00+01:00".zoned())
            }
        }
    }
}
