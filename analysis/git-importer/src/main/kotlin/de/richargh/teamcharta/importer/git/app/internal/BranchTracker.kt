package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api2.BranchAssignment
import de.richargh.teamcharta.importer.git.app.api2.CommitHash
import de.richargh.teamcharta.importer.git.app.api2.Ref

class BranchTracker {
    private val branchFor = mutableMapOf<CommitHash, BranchAssignment>()

    fun trackBranch(
        hash: CommitHash,
        refs: List<Ref>,
        parents: List<CommitHash>,
        message: String
    ): BranchAssignment? {
        val branch = byTipOrHead(hash, refs) ?: byRegistry(hash)

        registerParents(branch, parents, message)

        return branch
    }

    private fun registerParents(
        branch: BranchAssignment?,
        parents: List<CommitHash>,
        message: String
    ) {
        if (branch != null && parents.isNotEmpty()) {
            registerParent(parents.first(), branch)
        }

        if (parents.size >= 2) {
            val mergedParents = parents.drop(1)
            val mergedBranches = extractAllMergedBranches(message).map(BranchAssignment::Inferred)
            registerMergedParents(mergedParents, mergedBranches)
        }
    }

    private fun byTipOrHead(hash: CommitHash, refs: List<Ref>): BranchAssignment? {
        val branchRef = refs.filterIsInstance<Ref.BranchTip>().firstOrNull()
            ?: refs.filterIsInstance<Ref.Head>().firstOrNull()?.let { Ref.BranchTip(it.branch) }

        if (branchRef != null) {
            val assignment = BranchAssignment.Certain(branchRef.name)
            branchFor[hash] = assignment
            return assignment
        }
        return null
    }

    private fun registerParent(parentHash: CommitHash, childBranch: BranchAssignment) {
        if (parentHash !in branchFor) {
            branchFor[parentHash] = childBranch
        }
    }

    private fun registerMergedParents(
        mergedParents: List<CommitHash>,
        mergedBranches: List<BranchAssignment.Inferred>
    ) {
        mergedBranches.zip(mergedParents).forEach { (branch, parentHash) ->
            registerParent(parentHash, branch)
        }
    }

    private fun byRegistry(hash: CommitHash): BranchAssignment? = branchFor[hash]

}
