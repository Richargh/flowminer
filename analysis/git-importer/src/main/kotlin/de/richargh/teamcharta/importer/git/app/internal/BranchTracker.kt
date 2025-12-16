package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api.NameCertainty
import de.richargh.teamcharta.importer.git.app.api.CommitHash
import de.richargh.teamcharta.importer.git.app.api.Ref

data class TrackingResult(
    val branch: NameCertainty,
    val isOnActiveBranch: Boolean
)

class BranchTracker {
    private val branchFor = mutableMapOf<CommitHash, NameCertainty>()
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
            ?: NameCertainty.Nameless

        registerParents(branch, parents, message, hash in activeChain)

        return TrackingResult(branch, hash in activeChain)
    }

    private fun registerParents(
        branch: NameCertainty,
        parents: List<CommitHash>,
        message: String,
        isChildOnHeadChain: Boolean
    ) {
        if (branch != NameCertainty.Nameless && parents.isNotEmpty()) {
            registerFirstParent(parents.first(), branch, isChildOnHeadChain)
        }

        if (parents.size >= 2) {
            val mergedParents = parents.drop(1)
            val mergedBranches = extractAllMergedBranches(message).map(NameCertainty.Named::Inferred)
            registerMergedParents(mergedParents, mergedBranches)
        }
    }

    private fun byTipOrHead(hash: CommitHash, refs: List<Ref>): NameCertainty? {
        val branchRef = refs.filterIsInstance<Ref.BranchTip>().firstOrNull()
            ?: refs.filterIsInstance<Ref.LocalHead>().firstOrNull()?.let { Ref.BranchTip(it.branch) }

        if (branchRef != null) {
            val assignment = NameCertainty.Named.Certain(branchRef.name)
            branchFor[hash] = assignment
            return assignment
        }
        return null
    }

    private fun registerFirstParent(parentHash: CommitHash, childBranch: NameCertainty, isChildOnHeadChain: Boolean) {
        if (isChildOnHeadChain) {
            activeChain.add(parentHash)
            branchFor[parentHash] = childBranch
        } else if (parentHash !in activeChain && parentHash !in branchFor) {
            branchFor[parentHash] = childBranch
        }
    }

    private fun registerMergedParents(
        mergedParents: List<CommitHash>,
        mergedBranches: List<NameCertainty.Named.Inferred>
    ) {
        mergedBranches.zip(mergedParents).forEach { (branch, parentHash) ->
            registerMergedParent(parentHash, branch)
        }
    }

    private fun registerMergedParent(parentHash: CommitHash, branch: NameCertainty.Named.Inferred) {
        // Merged parents never overwrite HEAD chain or existing assignments
        if (parentHash !in activeChain && parentHash !in branchFor) {
            branchFor[parentHash] = branch
        }
    }

    private fun byRegistry(hash: CommitHash): NameCertainty? = branchFor[hash]

}
