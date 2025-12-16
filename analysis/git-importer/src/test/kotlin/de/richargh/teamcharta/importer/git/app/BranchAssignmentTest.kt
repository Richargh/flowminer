package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api.BranchNameCertainty
import de.richargh.teamcharta.importer.git.app.api.BranchName
import de.richargh.teamcharta.importer.git.app.api.aCommit
import de.richargh.teamcharta.importer.git.app.test.haveSameBranchAs
import de.richargh.teamcharta.importer.shared.time.app.atStartOfYear
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BranchAssignmentTest {

    @Test
    fun `should assign Certain branch from tip`() {
        // main: 0 (origin/main)

        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                authorDate(atStartOfYear(2024))
            }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.first().branch shouldBe BranchNameCertainty.Named.Certain(BranchName("origin/main"))
    }

    @Test
    fun `should propagate Certain to first parent`() {
        // main: 0───1 (origin/main)

        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                subject("First")
            }
            anEntry("origin/main") {
                subject("Second")
            }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then - both commits should be on main
        result[0] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("Second")
        })
        result[1] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("First")
        })
    }

    @Test
    fun `should propagate Certain to grand parents`() {
        // main: 0───1───2───3 (origin/main)

        // Given
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

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then - both commits should be on main
        result[0] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("Fourth")
        })
        result[1] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("Third")
        })
        result[2] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("Second")
        })
        result[3] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("First")
        })
    }

    @Test
    fun `should assign Certain to second parent, when feature branch still exists`() {
        // main:    0───1───3 (HEAD -> main, origin/main) [merge feature]
        //           \     /
        // feature:   └─2─┘

        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|3|1 2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feature' into origin/main
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feature|2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feature commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0	0	feature.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0	0	main.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result[0] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("Merge branch 'origin/feature' into origin/main")
        })
        result[1] should haveSameBranchAs(aCommit {
            certainBranch("origin/feature")
            message("feature commit")
        })
        result[2] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("main commit")
        })
        result[3] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("initial commit")
        })
    }

    @Test
    fun `should assign Inferred to second parent from merge message, when feature branch has been deleted`() {
        // main:    0───1───3 (HEAD -> main, origin/main) [merge feature]
        //           \     /
        // feature:   └─2─┘ (deleted)

        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|3|1 2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feature' into origin/main
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feature commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0	0	feature.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0	0	main.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result[0] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("Merge branch 'origin/feature' into origin/main")
        })
        result[1] should haveSameBranchAs(aCommit {
            inferredBranch("origin/feature")
            message("feature commit")
        })
        result[2] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("main commit")
        })
        result[3] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("initial commit")
        })
    }

    @Test
    fun `should assign null to second parent from merge message, when feature branch has been deleted and merge message non-standard`() {
        // main:    0───1───3 (HEAD -> main, origin/main) [merge feature]
        //           \     /
        // feature:   └─2─┘ (deleted)

        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|3|1 2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|Merge branch feature into main
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feature commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0	0	feature.md

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            0	0	main.md

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result[0] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("Merge branch feature into main")
        })
        result[1] should haveSameBranchAs(aCommit {
            nobranch()
            message("feature commit")
        })
        result[2] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("main commit")
        })
        result[3] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("initial commit")
        })
    }

    @Test
    fun `should assign branches, even when feature branches do not haven parents`() {
        // main:    1───2 (HEAD -> main, origin/main) [merge feature]
        //             /
        // feature: 0─┘ (origin/feature)

        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|2|1 0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feature' into origin/main
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1||2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit
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

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result[0] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("Merge branch 'origin/feature' into origin/main")
        })
        result[1] should haveSameBranchAs(aCommit {
            certainBranch("origin/main")
            message("main commit")
        })
        result[2] should haveSameBranchAs(aCommit {
            certainBranch("origin/feature")
            message("feature commit")
        })
    }

    @Test
    fun `should not overwrite Certain with Inferred`() {
        // main:        1───2 (HEAD -> main, origin/main) [merge feature]
        //             /
        // feature: 0─┘ (origin/feature)

        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|2|1 0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|Merge branch 'origin/feature' into origin/main
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

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then - commit 0 has branch ref, should stay Certain
        result[2] should haveSameBranchAs(aCommit {
            certainBranch("origin/feature")
            message("feature commit")
        })
    }

    @Test
    fun `should prioritize the HEAD branch, when a commit is in multiple possible branches, even when first commit is not on head branch`() {
        // trunk: 0───1───3 (HEAD -> trunk, origin/trunk) [merge feat]
        //         \     /
        // feat:    └──2───4 (origin/feat)

        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            origin/feat|4|2|2025-01-01T04:00:00+01:00|John Doe|john@example.com|feat2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            
            -----COMMIT_START-----
            HEAD -> trunk, origin/trunk, origin/HEAD|3|1 2|2025-01-01T03:00:00+01:00|John Doe|john@example.com|Merge branch 'feat' into trunk
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            
            -----COMMIT_START-----
            |2|0|2025-01-01T02:00:00+01:00|John Doe|john@example.com|feat1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            
            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|trunk2 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
            
            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|trunk1 commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result[0] should haveSameBranchAs(aCommit {
            certainBranch("origin/feat")
            message("feat2 commit")
        })
        result[1] should haveSameBranchAs(aCommit {
            certainBranch("origin/trunk")
            message("Merge branch 'feat' into trunk")
        })
        result[2] should haveSameBranchAs(aCommit {
            certainBranch("origin/feat")
            message("feat1 commit")
        })
        result[3] should haveSameBranchAs(aCommit {
            certainBranch("origin/trunk")
            message("trunk2 commit")
        })
        result[4] should haveSameBranchAs(aCommit {
            certainBranch("origin/trunk")
            message("trunk1 commit")
        })
    }

    @Test
    fun `should mark commits on HEAD chain as isOnActiveBranch`() {
        // main:    0───1───2 (HEAD -> main, origin/main)
        //           \
        // feature:   └─3 (origin/feature)

        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main|2|1|2025-01-01T02:00:00+01:00|John Doe|john@example.com|main commit 2
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            origin/feature|3|0|2025-01-01T01:30:00+01:00|John Doe|john@example.com|feature commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |1|0|2025-01-01T01:00:00+01:00|John Doe|john@example.com|main commit 1
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----

            -----COMMIT_START-----
            |0||2025-01-01T00:00:00+01:00|John Doe|john@example.com|initial commit
            -----BODY_START-----
            -----TRAILERS_START-----
            -----FILES_START-----
        """.trimIndent()

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then - commits reachable from HEAD should be marked
        result[0].isOnActiveBranch shouldBe true  // main commit 2 (HEAD)
        result[1].isOnActiveBranch shouldBe false // feature commit (not on HEAD chain)
        result[2].isOnActiveBranch shouldBe true  // main commit 1 (first parent of HEAD)
        result[3].isOnActiveBranch shouldBe true  // initial commit (ancestor of HEAD)
    }

    @Nested
    inner class OctopusMerge {

        @Test
        fun `should assign Inferred to all parents in octopus merge when all branches deleted`() {
            // main:  0───────4 (HEAD -> main, origin/main) [octopus merge]
            //       /|\     /|\
            // feat: 1 2 3──┘ │ │ (all deleted)
            //          └─────┘ │
            //            └─────┘

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|4|0 1 2 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Merge branches 'origin/feat1', 'origin/feat2' and 'origin/feat3'
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

            val testee = GitLogParser()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then
            result[0] should haveSameBranchAs(aCommit {
                certainBranch("origin/main")
                message("Merge branches 'origin/feat1', 'origin/feat2' and 'origin/feat3'")
            })
            result[1] should haveSameBranchAs(aCommit {
                inferredBranch("origin/feat3")
                message("feat3 commit")
            })
            result[2] should haveSameBranchAs(aCommit {
                inferredBranch("origin/feat2")
                message("feat2 commit")
            })
            result[3] should haveSameBranchAs(aCommit {
                inferredBranch("origin/feat1")
                message("feat1 commit")
            })
            result[4] should haveSameBranchAs(aCommit {
                certainBranch("origin/main")
                message("main commit")
            })
        }

        @Test
        fun `should assign Certain to all parents in octopus merge when all branches have tips`() {
            // main:  0───────4 (HEAD -> main, origin/main) [octopus merge]
            //       /|\     /|\
            // feat: 1 2 3──┘ │ │ (origin/feat1, origin/feat2, origin/feat3)
            //          └─────┘ │
            //            └─────┘

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|4|0 1 2 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Merge branches 'origin/feat1', 'origin/feat2' and 'origin/feat3'
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

            val testee = GitLogParser()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then - all branches have refs, so all are Certain
            result[1] should haveSameBranchAs(aCommit {
                certainBranch("origin/feat3")
                message("feat3 commit")
            })
            result[2] should haveSameBranchAs(aCommit {
                certainBranch("origin/feat2")
                message("feat2 commit")
            })
            result[3] should haveSameBranchAs(aCommit {
                certainBranch("origin/feat1")
                message("feat1 commit")
            })
        }

        @Test
        fun `should assign mixed Certain and Inferred in octopus merge when some branches have tips`() {
            // main:  0───────4 (HEAD -> main, origin/main) [octopus merge]
            //       /|\     /|\
            // feat: 1 2 3──┘ │ │ (origin/feat1 exists, feat2 & feat3 deleted)
            //          └─────┘ │
            //            └─────┘

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|4|0 1 2 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Merge branches 'origin/feat1', 'origin/feat2' and 'origin/feat3'
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

            val testee = GitLogParser()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then - feat1 has ref (Certain), feat2 and feat3 from message (Inferred)
            result[1] should haveSameBranchAs(aCommit {
                inferredBranch("origin/feat3")
                message("feat3 commit")
            })
            result[2] should haveSameBranchAs(aCommit {
                inferredBranch("origin/feat2")
                message("feat2 commit")
            })
            result[3] should haveSameBranchAs(aCommit {
                certainBranch("origin/feat1")
                message("feat1 commit")
            })
        }

        @Test
        fun `should assign mixed Certain and null in octopus merge when some branches have tips and merge-message is non-standard`() {
            // main:  0───────4 (HEAD -> main, origin/main) [octopus merge, non-standard message]
            //       /|\     /|\
            // feat: 1 2 3──┘ │ │ (origin/feat3 exists, feat1 & feat2 deleted)
            //          └─────┘ │
            //            └─────┘

            // Given
            val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|4|0 1 2 3|2025-01-01T04:00:00+01:00|John Doe|john@example.com|Combined feat1 feat2 feat3 into main
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

            val testee = GitLogParser()

            // When
            val result = testee.parse(gitLogContent.lineSequence())

            // Then - no branch refs, non-standard message = null branch
            result[1] should haveSameBranchAs(aCommit {
                certainBranch("origin/feat3")
                message("feat3 commit")
            })
            result[2].branch shouldBe BranchNameCertainty.Nameless
            result[3].branch shouldBe BranchNameCertainty.Nameless
        }
    }
}