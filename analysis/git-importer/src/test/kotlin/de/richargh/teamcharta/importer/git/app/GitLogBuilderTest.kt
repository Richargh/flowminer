package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api.hash
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GitLogBuilderTest {

    @Test
    fun `should auto-increment hash`(){
        // main:    0───
        // develop: 1───
        // stage:   2───
        // when
        val result = aGitLog {
            anEntry("origin/main"){
            }
            anEntry("origin/develop"){
            }
            anEntry("origin/stage"){
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            origin/stage|2||2024-01-15T09:00:00Z|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            origin/develop|1||2024-01-15T09:00:00Z|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            origin/main|0||2024-01-15T09:00:00Z|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should automatically link a commit to its parent`(){
        // main:    0───1
        // when
        val result = aGitLog {
            anEntry("origin/main"){
            }
            anEntry("origin/main"){
                author("John Min")
                authorMail("min@example.com")
                subject("Later commit")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            origin/main|1|0|2024-01-15T09:00:00Z|John Min|min@example.com|Later commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            |0||2024-01-15T09:00:00Z|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow branching off`(){
        // main:       0───2     (origin/main)
        //              \
        // feature-1:    1     (origin/feature-1)
        // when
        val result = aGitLog {
            anEntry("origin/main"){ }
            anEntry("origin/feature-1", "origin/main"){
                subject("Feature commit")
            }
            anEntry("origin/main"){
                subject("Latest commit")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            origin/main|2|0|2024-01-15T09:00:00Z|John Doe|john@example.com|Latest commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            origin/feature-1|1|0|2024-01-15T09:00:00Z|John Doe|john@example.com|Feature commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            |0||2024-01-15T09:00:00Z|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow branching off when setting the hash values manually`(){
        // main:       mmm123───mmm456     (origin/main)
        //              \
        // feature-1:    fff123     (origin/feature-1)
        // when
        val result = aGitLog {
            anEntry("origin/main"){
                hash("mmm123".hash())
            }
            anEntry("origin/feature-1", "origin/main"){
                hash("fff123".hash())
                subject("Feature commit")
            }
            anEntry("origin/main"){
                hash("mmm456".hash())
                subject("Latest commit")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            origin/main|mmm456|mmm123|2024-01-15T09:00:00Z|John Doe|john@example.com|Latest commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            origin/feature-1|fff123|mmm123|2024-01-15T09:00:00Z|John Doe|john@example.com|Feature commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            |mmm123||2024-01-15T09:00:00Z|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow merging back`(){
        // main:        0───1───2 (origin/main)
        //               \     /
        // feature-1:     └─1─┘  (origin/feature-1)
        // when
        val result = aGitLog {
            anEntry("origin/main"){ }
            anEntry("origin/feature-1", "origin/main"){
                subject("Feature commit")
            }
            anEntry("origin/main", "origin/feature-1"){
                subject("Latest commit")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            origin/main|2|0 1|2024-01-15T09:00:00Z|John Doe|john@example.com|Latest commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            origin/feature-1|1|0|2024-01-15T09:00:00Z|John Doe|john@example.com|Feature commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            |0||2024-01-15T09:00:00Z|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow merging back when setting the hash values manually`(){
        // when
        val result = aGitLog {
            anEntry("origin/main"){
                hash("mmm123".hash())
            }
            anEntry("origin/feature-1", "origin/main"){
                hash("fff123".hash())
                subject("Feature commit")
            }
            anEntry("origin/main", "origin/feature-1"){
                hash("mmm456".hash())
                subject("Latest commit")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            origin/main|mmm456|mmm123 fff123|2024-01-15T09:00:00Z|John Doe|john@example.com|Latest commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            origin/feature-1|fff123|mmm123|2024-01-15T09:00:00Z|John Doe|john@example.com|Feature commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            |mmm123||2024-01-15T09:00:00Z|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow merging back then adding a new commit`(){
        // when
        val result = aGitLog {
            anEntry("origin/main"){ }
            anEntry("origin/feature-1", "origin/main"){
                subject("Feature commit 1")
            }
            anEntry("origin/main", "origin/feature-1"){
                subject("Merge commit")
            }
            anEntry("origin/feature-1", "origin/main"){
                subject("Feature commit 2")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            origin/feature-1|3|2|2024-01-15T09:00:00Z|John Doe|john@example.com|Feature commit 2
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            origin/main|2|0 1|2024-01-15T09:00:00Z|John Doe|john@example.com|Merge commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            |1|0|2024-01-15T09:00:00Z|John Doe|john@example.com|Feature commit 1
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            |0||2024-01-15T09:00:00Z|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow setting head`(){
        // when
        val result = aGitLog {
            anEntry("origin/main"){ }
            anEntry("origin/main"){
                subject("Later commit")
                refHead("main")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|1|0|2024-01-15T09:00:00Z|John Doe|john@example.com|Later commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            |0||2024-01-15T09:00:00Z|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

    @Test
    fun `should allow setting tag`(){
        // when
        val result = aGitLog {
            anEntry("origin/main"){ }
            anEntry("origin/main"){
                subject("Later commit")
                refTag("v1.1.0")
            }
        }
        // then
        result shouldBe """
            -----COMMIT_START-----
            tag: v1.1.0, origin/main|1|0|2024-01-15T09:00:00Z|John Doe|john@example.com|Later commit
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            |0||2024-01-15T09:00:00Z|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            -----FILES_START-----

        """.trimIndent()
    }

}