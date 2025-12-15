package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api2.BranchAssignment
import de.richargh.teamcharta.importer.git.app.api2.CommitHash
import de.richargh.teamcharta.importer.git.app.api2.Ref

class BranchTracker {
    private val branchFor = mutableMapOf<CommitHash, BranchAssignment>()
    private val headChain = mutableSetOf<CommitHash>()

    fun trackBranch(
        hash: CommitHash,
        refs: List<Ref>,
        parents: List<CommitHash>,
        message: String
    ): BranchAssignment? {
        val hasHead = refs.any { it is Ref.Head }
        if (hasHead) {
            headChain.add(hash)
        }

        val branch = byTipOrHead(hash, refs) ?: byRegistry(hash)

        registerParents(branch, parents, message, hash in headChain)

        return branch
    }

    private fun registerParents(
        branch: BranchAssignment?,
        parents: List<CommitHash>,
        message: String,
        isChildOnHeadChain: Boolean
    ) {
        if (branch != null && parents.isNotEmpty()) {
            registerFirstParent(parents.first(), branch, isChildOnHeadChain)
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

    private fun registerFirstParent(parentHash: CommitHash, childBranch: BranchAssignment, isChildOnHeadChain: Boolean) {
        if (isChildOnHeadChain) {
            headChain.add(parentHash)
            branchFor[parentHash] = childBranch
        } else if (parentHash !in headChain && parentHash !in branchFor) {
            branchFor[parentHash] = childBranch
        }
    }

    private fun registerMergedParents(
        mergedParents: List<CommitHash>,
        mergedBranches: List<BranchAssignment.Inferred>
    ) {
        mergedBranches.zip(mergedParents).forEach { (branch, parentHash) ->
            registerMergedParent(parentHash, branch)
        }
    }

    private fun registerMergedParent(parentHash: CommitHash, branch: BranchAssignment.Inferred) {
        // Merged parents never overwrite HEAD chain or existing assignments
        if (parentHash !in headChain && parentHash !in branchFor) {
            branchFor[parentHash] = branch
        }
    }

    private fun byRegistry(hash: CommitHash): BranchAssignment? = branchFor[hash]

}
