package de.richargh.teamcharta.importer.git.app.api2

import java.time.ZonedDateTime

class BranchInfoBuilder {
    private var name: String = "feature-branch"
    private var firstCommitHash: String = "abc123"
    private var firstCommitDate: ZonedDateTime = ZonedDateTime.parse("2024-01-10T10:00:00+01:00")
    private var mergeCommitHash: String? = null
    private var mergeDate: ZonedDateTime? = null
    private var targetBranch: String? = null

    fun name(name: String) = apply { this.name = name }
    fun firstCommitHash(hash: String) = apply { this.firstCommitHash = hash }
    fun firstCommitDate(date: ZonedDateTime) = apply { this.firstCommitDate = date }
    fun mergeCommitHash(hash: String) = apply { this.mergeCommitHash = hash }
    fun mergeDate(date: ZonedDateTime) = apply { this.mergeDate = date }
    fun targetBranch(branch: String) = apply { this.targetBranch = branch }

    fun mergedInto(targetBranch: String, mergeDate: ZonedDateTime, mergeCommitHash: String) = apply {
        this.mergeCommitHash = mergeCommitHash
        this.mergeDate = mergeDate
        this.targetBranch = targetBranch
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
