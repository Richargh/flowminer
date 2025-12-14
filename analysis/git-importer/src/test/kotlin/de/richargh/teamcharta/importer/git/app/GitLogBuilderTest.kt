package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.hash
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GitLogBuilderTest {

    @Test
    fun `should auto-increment hash`(){
        // when
        val result = aGitLog {
            anEntry("main"){
            }
            anEntry("develop"){
            }
            anEntry("stage"){
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            main|0||2024-01-15T10:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            develop|1||2024-01-15T10:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            stage|2||2024-01-15T10:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should automatically link a commit to its parent`(){
        // when
        val result = aGitLog {
            anEntry("main"){
            }
            anEntry("main"){
                author("John Min")
                authorMail("min@example.com")
                subject("Later commit")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            |0||2024-01-15T10:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            main|1|0|2024-01-15T10:00+01:00|John Min|min@example.com|Later commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow branching off`(){
        // when
        val result = aGitLog {
            anEntry("main"){ }
            anEntry("feature-1", "main"){
                subject("Feature commit")
            }
            anEntry("main"){
                subject("Latest commit")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            |0||2024-01-15T10:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            feature-1|1|0|2024-01-15T10:00+01:00|John Doe|john@example.com|Feature commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            main|2|0|2024-01-15T10:00+01:00|John Doe|john@example.com|Latest commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow branching off when setting the hash values manually`(){
        // when
        val result = aGitLog {
            anEntry("main"){
                hash("mmm123".hash())
            }
            anEntry("feature-1", "main"){
                hash("fff123".hash())
                subject("Feature commit")
            }
            anEntry("main"){
                hash("mmm456".hash())
                subject("Latest commit")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            |mmm123||2024-01-15T10:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            feature-1|fff123|mmm123|2024-01-15T10:00+01:00|John Doe|john@example.com|Feature commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            main|mmm456|mmm123|2024-01-15T10:00+01:00|John Doe|john@example.com|Latest commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow merging back`(){
        // when
        val result = aGitLog {
            anEntry("main"){ }
            anEntry("feature-1", "main"){
                subject("Feature commit")
            }
            anEntry("main", "feature-1"){
                subject("Latest commit")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            |0||2024-01-15T10:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            feature-1|1|0|2024-01-15T10:00+01:00|John Doe|john@example.com|Feature commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            main|2|0 1|2024-01-15T10:00+01:00|John Doe|john@example.com|Latest commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow merging back when setting the hash values manually`(){
        // when
        val result = aGitLog {
            anEntry("main"){
                hash("mmm123".hash())
            }
            anEntry("feature-1", "main"){
                hash("fff123".hash())
                subject("Feature commit")
            }
            anEntry("main", "feature-1"){
                hash("mmm456".hash())
                subject("Latest commit")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            |mmm123||2024-01-15T10:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            feature-1|fff123|mmm123|2024-01-15T10:00+01:00|John Doe|john@example.com|Feature commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            main|mmm456|mmm123 fff123|2024-01-15T10:00+01:00|John Doe|john@example.com|Latest commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow merging back then adding a new commit`(){
        // when
        val result = aGitLog {
            anEntry("main"){ }
            anEntry("feature-1", "main"){
                subject("Feature commit 1")
            }
            anEntry("main", "feature-1"){
                subject("Merge commit")
            }
            anEntry("feature-1", "main"){
                subject("Feature commit 2")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            |0||2024-01-15T10:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            |1|0|2024-01-15T10:00+01:00|John Doe|john@example.com|Feature commit 1
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            main|2|0 1|2024-01-15T10:00+01:00|John Doe|john@example.com|Merge commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            feature-1|3|2|2024-01-15T10:00+01:00|John Doe|john@example.com|Feature commit 2
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow setting head`(){
        // when
        val result = aGitLog {
            anEntry("main"){ }
            anEntry("main"){
                subject("Later commit")
                refHead("main")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            |0||2024-01-15T10:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            HEAD -> main, main|1|0|2024-01-15T10:00+01:00|John Doe|john@example.com|Later commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow setting tag`(){
        // when
        val result = aGitLog {
            anEntry("main"){ }
            anEntry("main"){
                subject("Later commit")
                refTag("v1.1.0")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            |0||2024-01-15T10:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            tag: v1.1.0, main|1|0|2024-01-15T10:00+01:00|John Doe|john@example.com|Later commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

}