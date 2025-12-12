package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.CommitType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CommitTypeDetectionTest {

    @ParameterizedTest
    @ValueSource(strings = [
        "add login",
        "this feature is amazing",
        "this might cause a bug",
        "I think this was the fix"
    ])
    fun `should not detect commit type from message when none is there`(subject: String) {
        // Given
        val gitLogContent = aGitLog {
            anEntry("main") { subject(subject) }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitType shouldBe CommitType.UNKNOWN
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
        "feat!: add login",
        "feat(scope)!: add login",
        "feat!(scope): add login",
        "feature!: add login",
        "feature(scope)!: add login",
        "feature!(scope): add login",

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
        val gitLogContent = aGitLog {
            anEntry("main") { subject(subject) }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitType shouldBe CommitType.FEATURE
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
        "fix!: add login",
        "fix(scope)!: add login",
        "fix!(scope): add login",
        "bugfix!: add login",
        "bugfix(scope)!: add login",
        "bugfix!(scope): add login",

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
        val gitLogContent = aGitLog {
            anEntry("main") { subject(subject) }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitType shouldBe CommitType.FIX
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
        "refactor!: add login",
        "refactor(scope)!: add login",
        "refactor!(scope): add login",
        "refactoring!: add login",
        "refactoring(scope)!: add login",
        "refactoring!(scope): add login",

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
        val gitLogContent = aGitLog {
            anEntry("main") { subject(subject) }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitType shouldBe CommitType.REFACTOR
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
        "test!: add login",
        "test(scope)!: add login",
        "test!(scope): add login",
        "testing!: add login",
        "testing(scope)!: add login",
        "testing!(scope): add login",

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
        val gitLogContent = aGitLog {
            anEntry("main") { subject(subject) }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitType shouldBe CommitType.TEST
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "build: add login",
        "chore: add login",
        "ci: add login",
        "ops: add login",
        "E: add login",
        "e: add login",
        "build(scope): add login",
        "build[scope]: add login",
        "chore(scope): add login",
        "chore[scope]: add login",
        "ci(scope): add login",
        "ci[scope]: add login",
        "ops(scope): add login",
        "ops[scope]: add login",
        "build!: add login",
        "build(scope)!: add login",
        "build!(scope): add login",
        "chore!: add login",
        "chore(scope)!: add login",
        "chore!(scope): add login",

        "e: add login",
        "E: add login",
        "e(scope): add login",
        "e[scope]: add login",
        "E(scope): add login",
        "E[scope]: add login",
        ". e: add login",
        "^ e: add login",
        "@ e: add login",
        ". e(scope): add login",
        "^ e(scope): add login",
        "@ e(scope): add login",
        ". e[scope]: add login",
        "^ e[scope]: add login",
        "@ e[scope]: add login",

        "build     : add login",
        "chore   [scope]  : add login",
        "ci   (scope)   : add login",
        "E   [scope]  : add login",
        "@ e     : add login",
        "^ e  (scope)  : add login",
        "^ e   [scope]   : add login",
        "E  : add login",
        "e    add login",
        "   E  (scope): add login",
        "@ e[scope]   add login",
    ])
    fun `should detect ENVIRONMENT commit type from message`(subject: String) {
        // Given
        val gitLogContent = aGitLog {
            anEntry("main") { subject(subject) }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitType shouldBe CommitType.ENVIRONMENT
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "doc: add login",
        "docs: add login",
        "documentation: add login",
        "d: add login",
        "D: add login",
        "doc(scope): add login",
        "doc[scope]: add login",
        "docs(scope): add login",
        "docs[scope]: add login",
        "documentation(scope): add login",
        "documentation[scope]: add login",
        "d(scope): add login",
        "d[scope]: add login",
        "D(scope): add login",
        "D[scope]: add login",
        "doc!: add login",
        "doc(scope)!: add login",
        "doc!(scope): add login",
        "docs!: add login",
        "docs(scope)!: add login",
        "docs!(scope): add login",

        "d: add login",
        "D: add login",
        "d(scope): add login",
        "d[scope]: add login",
        "D(scope): add login",
        "D[scope]: add login",
        ". d: add login",
        "^ d: add login",
        "@ d: add login",
        ". d(scope): add login",
        "^ d(scope): add login",
        "@ d(scope): add login",
        ". d[scope]: add login",
        "^ d[scope]: add login",
        "@ d[scope]: add login",

        "doc     : add login",
        "docs   [scope]  : add login",
        "documentation   (scope)   : add login",
        "D   [scope]  : add login",
        "@ d     : add login",
        "^ d  (scope)  : add login",
        "^ d   [scope]   : add login",
        "D  : add login",
        "d   add login",
        "   D  (scope): add login",
        "@ d[scope]   add login",
    ])
    fun `should detect DOCS commit type from message`(subject: String) {
        // Given
        val gitLogContent = aGitLog {
            anEntry("main") { subject(subject) }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].commitType shouldBe CommitType.DOCS
    }
}
