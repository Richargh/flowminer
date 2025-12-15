package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.BranchAssignment
import de.richargh.teamcharta.importer.git.app.api2.BranchName
import de.richargh.teamcharta.importer.git.app.api2.aCommit
import de.richargh.teamcharta.importer.git.app.internal.atStartOfYear
import de.richargh.teamcharta.importer.git.app.internal.zoned
import de.richargh.teamcharta.importer.git.app.test.haveSameBranchAs
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BranchAssignmentTest {

    @Test
    fun `should assign Certain branch from tip`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                authorDate(atStartOfYear(2024))
            }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().branch shouldBe BranchAssignment.Certain(BranchName("origin/main"))
    }

    @Test
    fun `should propagate Certain to first parent`() {
        // Given - two commits on main, newest first in output
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                subject("First")
            }
            anEntry("origin/main") {
                subject("Second")
            }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then - both commits should be on main
        result.commits[0] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/main")))
            message("Second")
        })
        result.commits[1] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/main")))
            message("First")
        })
    }

    @Test
    fun `should propagate Certain to grand parents`() {
        // Given - two commits on main, newest first in output
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                subject("First")
            }
            anEntry("origin/main") {
                subject("Second")
            }
            anEntry("origin/main") {
                subject("Third")
            }
            anEntry("origin/main") {
                subject("Fourth")
            }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then - both commits should be on main
        result.commits[0] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/main")))
            message("Fourth")
        })
        result.commits[1] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/main")))
            message("Third")
        })
        result.commits[2] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/main")))
            message("Second")
        })
        result.commits[3] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/main")))
            message("First")
        })
    }

    @Test
    fun `should assign Certain to second parent from merge message, when branch tip still exists`() {
        // Given - merge commit with deleted feature branch
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> origin/main|2|1 0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feature' into origin/main
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0	0	main.md

            -----COMMIT_START-----
            origin/feature|0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|feature commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0	0	feature.md
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/main")))
            date("2025-01-01T02:00:00+01:00".zoned())
            message("Merge branch 'origin/feature' into origin/main")
        })
        result.commits[1] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/main")))
            date("2025-01-01T01:00:00+01:00".zoned())
            message("main commit")
        })
        result.commits[2] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/feature")))
            date("2025-01-01T00:00:00+01:00".zoned())
            message("feature commit")
        })
    }

    @Test
    fun `should assign Inferred to second parent from merge message, when feature branch has been deleted`() {
        // Given - merge commit with deleted feature branch
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> origin/main|2|1 0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feature' into origin/main
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0	0	main.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|feature commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0	0	feature.md
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/main")))
            date("2025-01-01T02:00:00+01:00".zoned())
            message("Merge branch 'origin/feature' into origin/main")
        })
        result.commits[1] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/main")))
            date("2025-01-01T01:00:00+01:00".zoned())
            message("main commit")
        })
        result.commits[2] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Inferred(BranchName("origin/feature")))
            date("2025-01-01T00:00:00+01:00".zoned())
            message("feature commit")
        })
    }

    @Test
    fun `should not overwrite Certain with Inferred`() {
        // Given - feature branch ref still exists
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> origin/main|2|1 0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feature' into origin/main
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feature|0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|feature commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then - commit 0 has branch ref, should stay Certain
        result.commits[2] should haveSameBranchAs(aCommit {
            branch(BranchAssignment.Certain(BranchName("origin/feature")))
            date("2025-01-01T00:00:00+01:00".zoned())
            message("feature commit")
        })
    }

    @Test
    fun `should assign null branch when feature deleted and merge message non-standard`() {
        // Given - regular merge with deleted branch and non-standard message
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> origin/main|2|1 0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|Integrated feature work
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1||2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|feature commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then - feature branch deleted, message doesn't match pattern = null
        result.commits[2] should haveSameBranchAs(aCommit {
            nobranch()
            date("2025-01-01T00:00:00+01:00".zoned())
            message("feature commit")
        })
    }

    @Nested
    inner class OctopusMerge {

        @Test
        fun `should assign Inferred to all parents in octopus merge when all branches deleted`() {
            // Given - octopus merge with 3 feature branches (all deleted)
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> origin/main|4|0 1 2 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Merge branches 'origin/feat1', 'origin/feat2' and 'origin/feat3'
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3||2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat3 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |2||2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1||2025-01-01T01:00:00+01:00|John Doe|john@example.com|feat1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

            val testee = GitLogParser2()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result.commits[0] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Certain(BranchName("origin/main")))
                date("2025-01-01T04:00:00+01:00".zoned())
                message("Merge branches 'origin/feat1', 'origin/feat2' and 'origin/feat3'")
            })
            result.commits[1] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Inferred(BranchName("origin/feat3")))
                date("2025-01-01T03:00:00+01:00".zoned())
                message("feat3 commit")
            })
            result.commits[2] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Inferred(BranchName("origin/feat2")))
                date("2025-01-01T02:00:00+01:00".zoned())
                message("feat2 commit")
            })
            result.commits[3] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Inferred(BranchName("origin/feat1")))
                date("2025-01-01T01:00:00+01:00".zoned())
                message("feat1 commit")
            })
            result.commits[4] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Certain(BranchName("origin/main")))
                date("2025-01-01T00:00:00+01:00".zoned())
                message("main commit")
            })
        }

        @Test
        fun `should assign Certain to all parents in octopus merge when all branches have tips`() {
            // Given - octopus merge with 3 feature branches (all still have refs)
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> origin/main|4|0 1 2 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Merge branches 'origin/feat1', 'origin/feat2' and 'origin/feat3'
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat3|3||2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat3 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat2|2||2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat1|1||2025-01-01T01:00:00+01:00|John Doe|john@example.com|feat1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

            val testee = GitLogParser2()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then - all branches have refs, so all are Certain
            result.commits[1] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Certain(BranchName("origin/feat3")))
                date("2025-01-01T03:00:00+01:00".zoned())
                message("feat3 commit")
            })
            result.commits[2] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Certain(BranchName("origin/feat2")))
                date("2025-01-01T02:00:00+01:00".zoned())
                message("feat2 commit")
            })
            result.commits[3] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Certain(BranchName("origin/feat1")))
                date("2025-01-01T01:00:00+01:00".zoned())
                message("feat1 commit")
            })
        }

        @Test
        fun `should assign mixed Certain and Inferred in octopus merge when some branches have tips`() {
            // Given - octopus merge: feat1 has ref, feat2 and feat3 deleted
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> origin/main|4|0 1 2 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Merge branches 'origin/feat1', 'origin/feat2' and 'origin/feat3'
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |3||2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat3 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |2||2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat1|1||2025-01-01T01:00:00+01:00|John Doe|john@example.com|feat1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

            val testee = GitLogParser2()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then - feat1 has ref (Certain), feat2 and feat3 from message (Inferred)
            result.commits[1] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Inferred(BranchName("origin/feat3")))
                date("2025-01-01T03:00:00+01:00".zoned())
                message("feat3 commit")
            })
            result.commits[2] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Inferred(BranchName("origin/feat2")))
                date("2025-01-01T02:00:00+01:00".zoned())
                message("feat2 commit")
            })
            result.commits[3] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Certain(BranchName("origin/feat1")))
                date("2025-01-01T01:00:00+01:00".zoned())
                message("feat1 commit")
            })
        }

        @Test
        fun `should assign mixed Certain and null in octopus merge when some branches have tips and merge-message is non-standard`() {
            // Given - octopus merge with non-standard message format
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> origin/main|4|0 1 2 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Combined feat1 feat2 feat3 into main
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feat3|3||2025-01-01T03:00:00+01:00|John Doe|john@example.com|feat3 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |2||2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1||2025-01-01T01:00:00+01:00|John Doe|john@example.com|feat1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

            val testee = GitLogParser2()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then - no branch refs, non-standard message = null branch
            result.commits[1] should haveSameBranchAs(aCommit {
                branch(BranchAssignment.Certain(BranchName("origin/feat3")))
                date("2025-01-01T03:00:00+01:00".zoned())
                message("feat3 commit")
            })
            result.commits[2].branch shouldBe null
            result.commits[3].branch shouldBe null
        }
    }
}
