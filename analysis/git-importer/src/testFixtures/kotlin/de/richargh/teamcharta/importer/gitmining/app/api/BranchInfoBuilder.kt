package de.richargh.teamcharta.importer.gitmining.app.api

import de.richargh.teamcharta.importer.git.app.api.BranchName
import de.richargh.teamcharta.importer.git.app.api.CommitHash
import java.time.ZonedDateTime

class BranchInfoBuilder {
    private var name: NameCertainty = NameCertainty.Certain(BranchName("feature-branch"))
    private var firstCommitHash: CommitHash = CommitHash("abc123")
    private var firstCommitDate: ZonedDateTime = ZonedDateTime.parse("2024-01-10T10:00:00+01:00")
    private var lastCommitHash: CommitHash? = null
    private var lastCommitDate: ZonedDateTime? = null
    private var mergeCommitHash: CommitHash? = null
    private var mergeDate: ZonedDateTime? = null
    private var targetBranch: BranchName? = null

    fun name(name: String) = apply { this.name = NameCertainty.Certain(BranchName(name)) }
    fun inferredName(name: String) = apply { this.name = NameCertainty.Inferred(BranchName(name)) }
    fun unNamed() = apply { this.name = NameCertainty.Nameless }

    fun firstCommitHash(hash: CommitHash) = apply { this.firstCommitHash = hash }
    fun firstCommitDate(date: ZonedDateTime) = apply { this.firstCommitDate = date }
    fun lastCommitHash(hash: CommitHash) = apply { this.lastCommitHash = hash }
    fun lastCommitDate(date: ZonedDateTime) = apply { this.lastCommitDate = date }
    fun mergeCommitHash(hash: CommitHash) = apply { this.mergeCommitHash = hash }
    fun mergeDate(date: ZonedDateTime) = apply { this.mergeDate = date }
    fun targetBranch(branch: String) = apply { this.targetBranch = BranchName(branch) }

    fun mergedInto(targetBranch: String, mergeDate: ZonedDateTime, mergeCommitHash: String) = apply {
        this.mergeCommitHash = CommitHash(mergeCommitHash)
        this.mergeDate = mergeDate
        this.targetBranch = BranchName(targetBranch)
    }

    fun build(): BranchInfo = BranchInfo(
        firstCommitHash = firstCommitHash,
        firstCommitDate = firstCommitDate,
        lastCommitHash = lastCommitHash ?: firstCommitHash,
        lastCommitDate = lastCommitDate ?: firstCommitDate,
        mergeCommitHash = mergeCommitHash,
        mergeDate = mergeDate,
        targetBranch = targetBranch,
        nameCertainty = name
    )
}

fun aBranch(block: BranchInfoBuilder.() -> Unit = {}): BranchInfo =
    BranchInfoBuilder().apply(block).build()
