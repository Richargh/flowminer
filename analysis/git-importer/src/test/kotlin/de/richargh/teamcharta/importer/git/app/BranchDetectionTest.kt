package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.aBranch
import de.richargh.teamcharta.importer.git.app.internal.atStartOfYear
import de.richargh.teamcharta.importer.git.app.internal.zoned
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BranchDetectionTest {

    @Test
    fun `should extract branch info`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry {
                hash("mmm123")
                authorDate(atStartOfYear(2024))
                branch("origin/main")
            }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 1
        result.branches["main"] shouldBe aBranch {
            name("main")
            firstCommitHash("mmm123")
            firstCommitDate(atStartOfYear(2024))
        }
    }

    @Test
    fun `should extract branch info from merge commits`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry {
                hash("fff123")
                authorDate(atStartOfYear(2024))
                branch("origin/feature-login")
            }
            anEntry {
                hash("mmm123")
                authorDate(atStartOfYear(2025))
                parents("mmm123", "fff123")
                headRef("main")
            }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 2
        result.branches["feature-login"] shouldBe aBranch {
            name("feature-login")
            firstCommitHash("fff123")
            firstCommitDate(atStartOfYear(2024))
            mergedInto("main", atStartOfYear(2025), "mmm123")
        }
    }

    @Test
    fun `should extract branch info when one branch is unmerged`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry {
                hash("fff123")
                authorDate(atStartOfYear(2023))
                branch("origin/feature-login")
            }
            anEntry {
                hash("fff456")
                authorDate(atStartOfYear(2024))
                branch("origin/feature-user")
            }
            anEntry {
                hash("mmm456")
                authorDate(atStartOfYear(2025))
                parents("mmm123", "fff123")
                headRef("main")
            }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.size() shouldBe 3
        result.branches["feature-login"] shouldBe aBranch {
            name("feature-login")
            firstCommitHash("fff123")
            firstCommitDate(atStartOfYear(2023))
            mergedInto("main", atStartOfYear(2025), "mmm456")
        }
    }
}
