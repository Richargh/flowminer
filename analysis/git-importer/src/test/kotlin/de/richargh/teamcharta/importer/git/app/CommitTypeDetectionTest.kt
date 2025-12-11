package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.CommitType
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CommitTypeDetectionTest {

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
}