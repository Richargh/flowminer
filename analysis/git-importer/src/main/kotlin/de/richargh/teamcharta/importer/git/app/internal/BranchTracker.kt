package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api.BranchNameCertainty
import de.richargh.teamcharta.importer.git.app.api.CommitHash
import de.richargh.teamcharta.importer.git.app.api.NamedBranch
import de.richargh.teamcharta.importer.git.app.api.NamelessBranch
import de.richargh.teamcharta.importer.git.app.api.Ref

data class TrackingResult(
    val branch: BranchNameCertainty,
    val isOnCurrentBranch: Boolean
)

class BranchTracker {
    private val branchFor = mutableMapOf<CommitHash, BranchNameCertainty>()
    private val currentChain = mutableSetOf<CommitHash>()

    fun trackBranch(
        hash: CommitHash,
        refs: List<Ref>,
        parents: List<CommitHash>,
        message: String
    ): TrackingResult {
        val hasHead = refs.any { it is Ref.LocalHead }
        if (hasHead) {
            currentChain.add(hash)
        }

        val branch = byTipOrHead(hash, refs)
            ?: byRegistry(hash)
            ?: NamelessBranch

        registerParents(branch, parents, message, hash in currentChain)

        return TrackingResult(branch, hash in currentChain)
    }

    private fun registerParents(
        branch: BranchNameCertainty,
        parents: List<CommitHash>,
        message: String,
        isChildOnHeadChain: Boolean
    ) {
        if (branch != NamelessBranch && parents.isNotEmpty()) {
            registerFirstParent(parents.first(), branch, isChildOnHeadChain)
        }

        if (parents.size >= 2) {
            val mergedParents = parents.drop(1)
            val mergedBranches = extractAllMergedBranches(message).map(NamedBranch::Inferred)
            registerMergedParents(mergedParents, mergedBranches)
        }
    }

    private fun byTipOrHead(hash: CommitHash, refs: List<Ref>): BranchNameCertainty? {
        val branchRef = refs.filterIsInstance<Ref.BranchTip>().firstOrNull()
            ?: refs.filterIsInstance<Ref.LocalHead>().firstOrNull()?.let { Ref.BranchTip(it.branch) }

        if (branchRef != null) {
            val assignment = NamedBranch.Certain(branchRef.name)
            branchFor[hash] = assignment
            return assignment
        }
        return null
    }

    private fun registerFirstParent(parentHash: CommitHash, childBranch: BranchNameCertainty, isChildOnHeadChain: Boolean) {
        if (isChildOnHeadChain) {
            currentChain.add(parentHash)
            branchFor[parentHash] = childBranch
        } else if (parentHash !in currentChain && parentHash !in branchFor) {
            branchFor[parentHash] = childBranch
        }
    }

    private fun registerMergedParents(
        mergedParents: List<CommitHash>,
        mergedBranches: List<NamedBranch.Inferred>
    ) {
        mergedBranches.zip(mergedParents).forEach { (branch, parentHash) ->
            registerMergedParent(parentHash, branch)
        }
    }

    private fun registerMergedParent(parentHash: CommitHash, branch: NamedBranch.Inferred) {
        // Merged parents never overwrite HEAD chain or existing assignments
        if (parentHash !in currentChain && parentHash !in branchFor) {
            branchFor[parentHash] = branch
        }
    }

    private fun byRegistry(hash: CommitHash): BranchNameCertainty? = branchFor[hash]

}
