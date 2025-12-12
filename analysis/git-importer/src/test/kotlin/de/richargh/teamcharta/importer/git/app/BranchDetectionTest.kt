package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.aBranch
import de.richargh.teamcharta.importer.git.app.internal.zoned
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BranchDetectionTest {

    @Test
    fun `should extract branch info`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry {
                hash("abc123")
                authorDate("2024-01-10T10:00:00+01:00")
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
            firstCommitHash("abc123")
            firstCommitDate("2024-01-10T10:00:00+01:00".zoned())
        }
    }

    @Test
    fun `should extract branch info from merge commits`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry {
                hash("feat1")
                authorDate("2024-01-10T10:00:00+01:00")
                branch("origin/feature-login")
            }
            anEntry {
                hash("main1")
                authorDate("2024-01-12T10:00:00+01:00")
                parents("main1", "feat1")
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
            firstCommitHash("feat1")
            firstCommitDate("2024-01-10T10:00:00+01:00".zoned())
            mergedInto("main", "2024-01-12T10:00:00+01:00".zoned(), "main1")
        }
    }

    @Test
    fun `should extract branch info when one branch is unmerged`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry {
                hash("feat1")
                authorDate("2024-01-10T10:00:00+01:00")
                branch("origin/feature-login")
            }
            anEntry {
                hash("feat2")
                authorDate("2024-01-10T10:00:00+01:00")
                branch("origin/feature-user")
            }
            anEntry {
                hash("main1")
                authorDate("2024-01-12T10:00:00+01:00")
                parents("main1", "feat1")
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
            firstCommitHash("feat1")
            firstCommitDate("2024-01-10T10:00:00+01:00".zoned())
            mergedInto("main", "2024-01-12T10:00:00+01:00".zoned(), "main1")
        }
    }
}
