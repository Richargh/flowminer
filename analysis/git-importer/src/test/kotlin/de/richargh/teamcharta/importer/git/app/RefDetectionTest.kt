package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.Ref
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RefDetectionTest {

    @Test
    fun `should parse HEAD ref`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-10T10:00:00+01:00
            subject==>> Initial commit
            parents==>>
            refs==>> HEAD -> main
            -----BODY_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(
            Ref.Head("main")
        )
    }

    @Test
    fun `should parse tag ref`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-10T10:00:00+01:00
            subject==>> Release v1.0.0
            parents==>>
            refs==>> tag: v1.0.0
            -----BODY_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(
            Ref.Tag("v1.0.0")
        )
    }

    @Test
    fun `should parse branch ref without slash`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-10T10:00:00+01:00
            subject==>> Initial commit
            parents==>>
            refs==>> develop
            -----BODY_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(
            Ref.Branch("develop")
        )
    }

    @ParameterizedTest(name = "should parse nested branch ref: {0}")
    @CsvSource(
        "origin/add-user",
        "origin/feat/add-login",
        "origin/fix/at-1234",
        "feature/user/profile",
        "release/v2.0/hotfix",
        "bugfix/JIRA-123/fix-null-pointer",
        "upstream/develop/experimental"
    )
    fun `should parse nested branch refs`(branchName: String) {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-10T10:00:00+01:00
            subject==>> Initial commit
            parents==>>
            refs==>> $branchName
            -----BODY_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(
            Ref.Branch(branchName)
        )
    }

    @Test
    fun `should parse multiple refs`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-10T10:00:00+01:00
            subject==>> Release v1.0.0
            parents==>>
            refs==>> HEAD -> main, origin/main, tag: v1.0.0
            -----BODY_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(
            Ref.Head("main"),
            Ref.Branch("origin/main"),
            Ref.Tag("v1.0.0")
        )
    }

    @Test
    fun `should handle empty refs`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-10T10:00:00+01:00
            subject==>> Initial commit
            parents==>>
            refs==>>
            -----BODY_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldHaveSize 0
    }
}
