package de.richargh.flowminer.importer.git.app.internal

import de.richargh.flowminer.importer.git.app.api.BranchId
import de.richargh.flowminer.importer.git.app.api.CommitHash
import de.richargh.flowminer.importer.git.app.api.NamedBranchId
import de.richargh.flowminer.importer.git.app.api.NamelessBranchId
import de.richargh.flowminer.importer.git.app.api.Ref

data class TrackingResult(
    val branch: BranchId,
    val isOnCurrentBranch: Boolean
)

class BranchTracker {
    private val branchFor = mutableMapOf<CommitHash, BranchId>()
    private val currentChain = mutableSetOf<CommitHash>()
    private val upcomingBranchId = mutableMapOf<CommitHash, BranchId>()

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

        // Lookup or create BranchId
        var branchId: BranchId = upcomingBranchId.remove(hash)
            ?: NamelessBranchId(hash)  // tipCommit = this commit's hash

        // Override with named branch if available
        val namedBranch = byTipOrHead(hash, refs)
        if (namedBranch != null) {
            branchId = namedBranch
        } else {
            // Check for inferred name from registry
            val registeredBranch = byRegistry(hash)
            if (registeredBranch != null) {
                branchId = registeredBranch
            }
        }

        registerParents(branchId, parents, message, hash in currentChain)

        return TrackingResult(
            branchId,
            hash in currentChain
        )
    }

    private fun registerParents(
        branch: BranchId,
        parents: List<CommitHash>,
        message: String,
        isChildOnHeadChain: Boolean
    ) {
        // Propagate to first parent
        if (parents.isNotEmpty()) {
            upcomingBranchId[parents.first()] = branch
            if (branch is NamedBranchId) {
                registerFirstParent(parents.first(), branch, isChildOnHeadChain)
            }
        }

        // For merges, each merged parent starts a new nameless branch
        if (parents.size >= 2) {
            val mergedParents = parents.drop(1)
            val mergedBranches = extractAllMergedBranches(
                message
            ).map(NamedBranchId::Inferred)
            registerMergedParents(mergedParents, mergedBranches)

            // Register nameless branches for merged parents without inferred names
            mergedParents.drop(mergedBranches.size).forEach { parentHash ->
                upcomingBranchId[parentHash] =
                    NamelessBranchId(parentHash)
            }
        }
    }

    private fun byTipOrHead(hash: CommitHash, refs: List<Ref>): BranchId? {
        val branchRef = refs.filterIsInstance<Ref.BranchTip>().firstOrNull()
            ?: refs.filterIsInstance<Ref.LocalHead>().firstOrNull()?.let { Ref.BranchTip(it.branch) }

        if (branchRef != null) {
            val assignment = NamedBranchId.Certain(branchRef.name)
            branchFor[hash] = assignment
            return assignment
        }
        return null
    }

    private fun registerFirstParent(parentHash: CommitHash, childBranch: BranchId, isChildOnHeadChain: Boolean) {
        if (isChildOnHeadChain) {
            currentChain.add(parentHash)
            branchFor[parentHash] = childBranch
        } else if (parentHash !in currentChain && parentHash !in branchFor) {
            branchFor[parentHash] = childBranch
        }
    }

    private fun registerMergedParents(
        mergedParents: List<CommitHash>,
        mergedBranches: List<NamedBranchId.Inferred>
    ) {
        mergedBranches.zip(mergedParents).forEach { (branch, parentHash) ->
            registerMergedParent(parentHash, branch)
        }
    }

    private fun registerMergedParent(parentHash: CommitHash, branch: NamedBranchId.Inferred) {
        // Merged parents never overwrite HEAD chain or existing assignments
        if (parentHash !in currentChain && parentHash !in branchFor) {
            branchFor[parentHash] = branch
        }
    }

    private fun byRegistry(hash: CommitHash): BranchId? = branchFor[hash]

}
