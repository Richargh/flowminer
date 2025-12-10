package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.*
import de.richargh.teamcharta.importer.git.app.api2.CommitType
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.ZonedDateTime

class GitLogParser2Test {

    @Test
    fun `should parse single commit with numstat into Commit object`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00:00+01:00
            subject==>> Initial commit
            parents==>> parent1
            refs==>> HEAD -> main
            -----BODY_START-----
            This is the commit body.
            -----FILES_START-----
            5	2	src/Main.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits shouldContainExactly listOf(aCommit {
            hash("abc123")
            author("John Doe", "john@example.com")
            date(ZonedDateTime.parse("2024-01-15T10:00:00+01:00"))
            message("Initial commit")
            parents("parent1")
            refs("HEAD -> main")
            fileChanges(
                FileChange("src/Main.kt", additions = 5, deletions = 2))
        })
    }

    @Test
    fun `should extract trailers section`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc789
            author==>> Jane Smith
            authorMail==>> jane@example.com
            authorDate==>> 2024-01-17T09:00:00+01:00
            subject==>> Reviewed commit
            parents==>> def456
            refs==>>
            -----BODY_START-----
            Some changes.
            -----TRAILERS_START-----
            Co-authored-by: John Doe <john@example.com>
            Signed-off-by: Alice Wonder <alice@example.com>
            Reviewed-by: Bob Builder <bob@example.com>
            -----FILES_START-----
            3	1	src/File.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].trailers shouldContainExactly listOf(
            "Co-authored-by" to "John Doe <john@example.com>",
            "Signed-off-by" to "Alice Wonder <alice@example.com>",
            "Reviewed-by" to "Bob Builder <bob@example.com>"
        )
    }

    @Test
    fun `should extract co-authors from trailers section`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> def456
            author==>> Jane Smith
            authorMail==>> jane@example.com
            authorDate==>> 2024-01-16T14:30:00+01:00
            subject==>> Pair programming commit
            parents==>> abc123
            refs==>>
            -----BODY_START-----
            Added new feature together.
            -----TRAILERS_START-----
            Co-authored-by: John Doe <john@example.com>
            Co-authored-by: Alice Wonder <alice@example.com>
            -----FILES_START-----
            10	5	src/Feature.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].trailers shouldContainExactly listOf(
            "Co-authored-by" to "John Doe <john@example.com>",
            "Co-authored-by" to "Alice Wonder <alice@example.com>"
        )
        result.commits[0].coAuthors shouldContainExactly listOf(
            Author("John Doe", "john@example.com"),
            Author("Alice Wonder", "alice@example.com")
        )
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "add login",
    ])
    fun `should not detect commit type from message when none is there`(subject: String) {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00:00+01:00
            subject==>> $subject
            parents==>> parent1
            refs==>>
            -----BODY_START-----
            -----FILES_START-----
            5	2	src/Login.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitTypes.shouldBeEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "feat: add login",
        "feature: add login",
        "f: add login",
        "F: add login",
        "feat(scope): add login",
        "feat[scope]: add login",
        "feature(scope): add login",
        "feature[scope]: add login",

        "f: add login",
        "F: add login",
        "f(scope): add login",
        "f[scope]: add login",
        "F(scope): add login",
        "F[scope]: add login",
        ". f: add login",
        "^ f: add login",
        "@ f: add login",
        ". f(scope): add login",
        "^ f(scope): add login",
        "@ f(scope): add login",
        ". f[scope]: add login",
        "^ f[scope]: add login",
        "@ f[scope]: add login",

        "feature     : add login",
        "feat   [scope]  : add login",
        "feature   (scope)   : add login",
        "F   [scope]  : add login",
        "@ f     : add login",
        "^ f  (scope)  : add login",
        "^ f   [scope]   : add login",
        "F  : add login",
        "f    add login",
        "   F  (scope): add login",
        "@ f[scope]   add login",
    ])
    fun `should detect FEATURE commit type from message`(subject: String) {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00:00+01:00
            subject==>> $subject
            parents==>> parent1
            refs==>>
            -----BODY_START-----
            -----FILES_START-----
            5	2	src/Login.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitTypes shouldContainExactly listOf(CommitType.FEATURE)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "fix: add login",
        "bug: add login",
        "bugfix: add login",
        "hotfix: add login",
        "b: add login",
        "B: add login",
        "fix(scope): add login",
        "fix[scope]: add login",
        "bug(scope): add login",
        "bug[scope]: add login",
        "bugfix(scope): add login",
        "bugfix[scope]: add login",
        "hotfix(scope): add login",
        "hotfix[scope]: add login",

        "b: add login",
        "B: add login",
        "b(scope): add login",
        "b[scope]: add login",
        "B(scope): add login",
        "B[scope]: add login",
        ". b: add login",
        "^ b: add login",
        "@ b: add login",
        ". b(scope): add login",
        "^ b(scope): add login",
        "@ b(scope): add login",
        ". b[scope]: add login",
        "^ b[scope]: add login",
        "@ b[scope]: add login",

        "fix     : add login",
        "bug   [scope]  : add login",
        "bugfix   (scope)   : add login",
        "B   [scope]  : add login",
        "@ b     : add login",
        "^ b  (scope)  : add login",
        "^ b   [scope]   : add login",
        "B  : add login",
        "b    add login",
        "   B  (scope): add login",
        "@ b[scope]   add login",
    ])
    fun `should detect FIX commit type from message`(subject: String) {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00:00+01:00
            subject==>> $subject
            parents==>> parent1
            refs==>>
            -----BODY_START-----
            -----FILES_START-----
            5	2	src/Login.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitTypes shouldContainExactly listOf(CommitType.FIX)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "refactor: add login",
        "refactoring: add login",
        "r: add login",
        "R: add login",
        "refactor(scope): add login",
        "refactor[scope]: add login",
        "refactoring(scope): add login",
        "refactoring[scope]: add login",

        "r: add login",
        "R: add login",
        "r(scope): add login",
        "r[scope]: add login",
        "R(scope): add login",
        "R[scope]: add login",
        ". r: add login",
        "^ r: add login",
        "@ r: add login",
        ". r(scope): add login",
        "^ r(scope): add login",
        "@ r(scope): add login",
        ". r[scope]: add login",
        "^ r[scope]: add login",
        "@ r[scope]: add login",

        "refactor     : add login",
        "refactoring   [scope]  : add login",
        "refactor   (scope)   : add login",
        "R   [scope]  : add login",
        "@ r     : add login",
        "^ r  (scope)  : add login",
        "^ r   [scope]   : add login",
        "R  : add login",
        "r    add login",
        "   R  (scope): add login",
        "@ r[scope]   add login",
    ])
    fun `should detect REFACTOR commit type from message`(subject: String) {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00:00+01:00
            subject==>> $subject
            parents==>> parent1
            refs==>>
            -----BODY_START-----
            -----FILES_START-----
            5	2	src/Login.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitTypes shouldContainExactly listOf(CommitType.REFACTOR)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "test: add login",
        "testing: add login",
        "t: add login",
        "T: add login",
        "test(scope): add login",
        "test[scope]: add login",
        "testing(scope): add login",
        "testing[scope]: add login",

        "t: add login",
        "T: add login",
        "t(scope): add login",
        "t[scope]: add login",
        "T(scope): add login",
        "T[scope]: add login",
        ". t: add login",
        "^ t: add login",
        "@ t: add login",
        ". t(scope): add login",
        "^ t(scope): add login",
        "@ t(scope): add login",
        ". t[scope]: add login",
        "^ t[scope]: add login",
        "@ t[scope]: add login",

        "test     : add login",
        "testing   [scope]  : add login",
        "test   (scope)   : add login",
        "T   [scope]  : add login",
        "@ t     : add login",
        "^ t  (scope)  : add login",
        "^ t   [scope]   : add login",
        "T  : add login",
        "t    add login",
        "   T  (scope): add login",
        "@ t[scope]   add login",
    ])
    fun `should detect TEST commit type from message`(subject: String) {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00:00+01:00
            subject==>> $subject
            parents==>> parent1
            refs==>>
            -----BODY_START-----
            -----FILES_START-----
            5	2	src/Login.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitTypes shouldContainExactly listOf(CommitType.TEST)
    }

    @Test
    fun `should deduplicate co-authors with different trailer key variations`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> xyz999
            author==>> Jane Smith
            authorMail==>> jane@example.com
            authorDate==>> 2024-01-18T11:00:00+01:00
            subject==>> Commit with duplicate co-authors
            parents==>> abc123
            refs==>>
            -----BODY_START-----
            Some work.
            -----TRAILERS_START-----
            Co-authored-by: John Doe <john@example.com>
            co-authored-by: John Doe <john@example.com>
            Co-Authored-By: John Doe <john@example.com>
            Co-Authored By: John Doe <john@example.com>
            Co-Authored by: John Doe <john@example.com>
            -----FILES_START-----
            1	1	src/File.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].coAuthors shouldContainExactly listOf(
            Author("John Doe", "john@example.com")
        )
    }
}
