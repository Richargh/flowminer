package de.richargh.teamcharta.importer.git.app.api2

import java.time.ZonedDateTime

class BranchInfoBuilder {
    private var name: Ref.Branch = Ref.Branch("feature-branch")
    private var firstCommitHash: CommitHash = CommitHash("abc123")
    private var firstCommitDate: ZonedDateTime = ZonedDateTime.parse("2024-01-10T10:00:00+01:00")
    private var mergeCommitHash: CommitHash? = null
    private var mergeDate: ZonedDateTime? = null
    private var targetBranch: Ref.Branch? = null

    fun name(name: String) = apply { this.name = Ref.Branch(name) }
    fun firstCommitHash(hash: CommitHash) = apply { this.firstCommitHash = hash }
    fun firstCommitDate(date: ZonedDateTime) = apply { this.firstCommitDate = date }
    fun mergeCommitHash(hash: CommitHash) = apply { this.mergeCommitHash = hash }
    fun mergeDate(date: ZonedDateTime) = apply { this.mergeDate = date }
    fun targetBranch(branch: String) = apply { this.targetBranch = Ref.Branch(branch) }

    fun mergedInto(targetBranch: String, mergeDate: ZonedDateTime, mergeCommitHash: String) = apply {
        this.mergeCommitHash = CommitHash(mergeCommitHash)
        this.mergeDate = mergeDate
        this.targetBranch = Ref.Branch(targetBranch)
    }

    fun build(): BranchInfo = BranchInfo(
        name = name,
        firstCommitHash = firstCommitHash,
        firstCommitDate = firstCommitDate,
        mergeCommitHash = mergeCommitHash,
        mergeDate = mergeDate,
        targetBranch = targetBranch
    )
}

fun aBranch(block: BranchInfoBuilder.() -> Unit = {}): BranchInfo =
    BranchInfoBuilder().apply(block).build()
