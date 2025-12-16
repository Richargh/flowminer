package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api.BranchNameCertainty
import de.richargh.teamcharta.importer.git.app.api.CommitHash
import de.richargh.teamcharta.importer.git.app.api.Ref

data class TrackingResult(
    val branch: BranchNameCertainty,
    val isOnActiveBranch: Boolean
)

class BranchTracker {
    private val branchFor = mutableMapOf<CommitHash, BranchNameCertainty>()
    private val activeChain = mutableSetOf<CommitHash>()

    fun trackBranch(
        hash: CommitHash,
        refs: List<Ref>,
        parents: List<CommitHash>,
        message: String
    ): TrackingResult {
        val hasHead = refs.any { it is Ref.LocalHead }
        if (hasHead) {
            activeChain.add(hash)
        }

        val branch = byTipOrHead(hash, refs)
            ?: byRegistry(hash)
            ?: BranchNameCertainty.Nameless

        registerParents(branch, parents, message, hash in activeChain)

        return TrackingResult(branch, hash in activeChain)
    }

    private fun registerParents(
        branch: BranchNameCertainty,
        parents: List<CommitHash>,
        message: String,
        isChildOnHeadChain: Boolean
    ) {
        if (branch != BranchNameCertainty.Nameless && parents.isNotEmpty()) {
            registerFirstParent(parents.first(), branch, isChildOnHeadChain)
        }

        if (parents.size >= 2) {
            val mergedParents = parents.drop(1)
            val mergedBranches = extractAllMergedBranches(message).map(BranchNameCertainty.Named::Inferred)
            registerMergedParents(mergedParents, mergedBranches)
        }
    }

    private fun byTipOrHead(hash: CommitHash, refs: List<Ref>): BranchNameCertainty? {
        val branchRef = refs.filterIsInstance<Ref.BranchTip>().firstOrNull()
            ?: refs.filterIsInstance<Ref.LocalHead>().firstOrNull()?.let { Ref.BranchTip(it.branch) }

        if (branchRef != null) {
            val assignment = BranchNameCertainty.Named.Certain(branchRef.name)
            branchFor[hash] = assignment
            return assignment
        }
        return null
    }

    private fun registerFirstParent(parentHash: CommitHash, childBranch: BranchNameCertainty, isChildOnHeadChain: Boolean) {
        if (isChildOnHeadChain) {
            activeChain.add(parentHash)
            branchFor[parentHash] = childBranch
        } else if (parentHash !in activeChain && parentHash !in branchFor) {
            branchFor[parentHash] = childBranch
        }
    }

    private fun registerMergedParents(
        mergedParents: List<CommitHash>,
        mergedBranches: List<BranchNameCertainty.Named.Inferred>
    ) {
        mergedBranches.zip(mergedParents).forEach { (branch, parentHash) ->
            registerMergedParent(parentHash, branch)
        }
    }

    private fun registerMergedParent(parentHash: CommitHash, branch: BranchNameCertainty.Named.Inferred) {
        // Merged parents never overwrite HEAD chain or existing assignments
        if (parentHash !in activeChain && parentHash !in branchFor) {
            branchFor[parentHash] = branch
        }
    }

    private fun byRegistry(hash: CommitHash): BranchNameCertainty? = branchFor[hash]

}
