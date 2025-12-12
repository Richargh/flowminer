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
            hash==>> 0
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Initial commit
            parents==>> 
            refs==>> main
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> 1
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Initial commit
            parents==>> 
            refs==>> develop
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> 2
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Initial commit
            parents==>> 
            refs==>> stage
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
            hash==>> 0
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Initial commit
            parents==>> 
            refs==>> 
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> 1
            author==>> John Min
            authorMail==>> min@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Later commit
            parents==>> 0
            refs==>> main
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
            hash==>> 0
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Initial commit
            parents==>> 
            refs==>> 
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> 1
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Feature commit
            parents==>> 0
            refs==>> feature-1
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> 2
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Latest commit
            parents==>> 0
            refs==>> main
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
            hash==>> mmm123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Initial commit
            parents==>> 
            refs==>> 
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> fff123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Feature commit
            parents==>> mmm123
            refs==>> feature-1
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> mmm456
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Latest commit
            parents==>> mmm123
            refs==>> main
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
            hash==>> 0
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Initial commit
            parents==>> 
            refs==>> 
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> 1
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Feature commit
            parents==>> 0
            refs==>> feature-1
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> 2
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Latest commit
            parents==>> 1 0
            refs==>> main
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
            hash==>> mmm123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Initial commit
            parents==>> 
            refs==>> 
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> fff123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Feature commit
            parents==>> mmm123
            refs==>> feature-1
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> mmm456
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Latest commit
            parents==>> fff123 mmm123
            refs==>> main
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
            hash==>> 0
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Initial commit
            parents==>> 
            refs==>> 
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> 1
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Later commit
            parents==>> 0
            refs==>> HEAD -> main, main
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
            hash==>> 0
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Initial commit
            parents==>> 
            refs==>> 
            -----BODY_START-----
            -----FILES_START-----
            -----COMMIT_START-----
            hash==>> 1
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00+01:00
            subject==>> Later commit
            parents==>> 0
            refs==>> tag: v1.1.0, main
            -----BODY_START-----
            -----FILES_START-----
            
        """.trimIndent()
    }

}