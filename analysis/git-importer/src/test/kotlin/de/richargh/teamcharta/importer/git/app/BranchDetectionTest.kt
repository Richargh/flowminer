package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.aBranch
import de.richargh.teamcharta.importer.git.app.internal.zoned
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BranchDetectionTest {

    @Test
    fun `should extract branch info`() {
        // Given: Feature branch commit followed by merge commit
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> Jane Doe
            authorMail==>> jane@example.com
            authorDate==>> 2024-01-10T10:00:00+01:00
            subject==>> feat: add login
            parents==>> main1
            refs==>> origin/main
            -----BODY_START-----
            -----FILES_START-----
            10	0	src/Login.kt
        """.trimIndent()

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
        // Given: Feature branch commit followed by merge commit
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> feat1
            author==>> Jane Doe
            authorMail==>> jane@example.com
            authorDate==>> 2024-01-10T10:00:00+01:00
            subject==>> feat: add login
            parents==>> main1
            refs==>> origin/feature-login
            -----BODY_START-----
            -----FILES_START-----
            10	0	src/Login.kt
            -----COMMIT_START-----
            hash==>> main1
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-12T10:00:00+01:00
            subject==>> Merge branch 'feature-login' into main
            parents==>> main1 feat1
            refs==>> HEAD -> main
            -----BODY_START-----
            -----FILES_START-----
        """.trimIndent()

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
        // Given: Feature branch commit followed by merge commit
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> feat1
            author==>> Jane Doe
            authorMail==>> jane@example.com
            authorDate==>> 2024-01-10T10:00:00+01:00
            subject==>> feat: add login
            parents==>> main1
            refs==>> origin/feature-login
            -----BODY_START-----
            -----FILES_START-----
            10	0	src/Login.kt
            -----COMMIT_START-----
            hash==>> feat2
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-10T10:00:00+01:00
            subject==>> feat: add user
            parents==>> main1
            refs==>> origin/feature-user
            -----BODY_START-----
            -----FILES_START-----
            10	0	src/User.kt
            -----COMMIT_START-----
            hash==>> main1
            author==>> Alex Taylor
            authorMail==>> alex@example.com
            authorDate==>> 2024-01-12T10:00:00+01:00
            subject==>> Merge branch 'feature-login' into main
            parents==>> main1 feat1
            refs==>> HEAD -> main
            -----BODY_START-----
            -----FILES_START-----
        """.trimIndent()

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