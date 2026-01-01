import type {BranchId, Commit} from '../api-types/commit.ts';
import {type Branch, Branches, type BranchStatus} from "../api-types/branch.ts";

interface MutableBranch {
  branchId: BranchId;
  commits: Set<string>;
  firstCommitHash: string;
  firstCommitDate: Date;
  lastCommitHash: string;
  lastCommitDate: Date;
  mergeCommitHash: string | null;
  mergeCommitDate: Date | null;
  targetBranch: string | null;
  mergedParentHash: string | null;
  isCurrent: boolean;
}

export class BranchMiner {
  private readonly commitByHash: Map<string, Commit> = new Map();
  private readonly branches: Map<string, MutableBranch> = new Map();
  private readonly pendingMerges: Commit[] = [];

  process(commit: Commit): void {
    this.commitByHash.set(commit.hash, commit);
    this.addToBranch(commit);

    if (commit.isMerge) {
      this.pendingMerges.push(commit);
    }
  }

  getResult(currentDate: Date): Branches {
    // Process pending merges
    for (const mergeCommit of this.pendingMerges) {
      this.processMerge(mergeCommit);
    }

    const branches: Branch[] = [];
    for (const mutable of this.branches.values()) {
      branches.push(this.toBranch(mutable, currentDate));
    }

    return new Branches(branches);
  }

  private addToBranch(commit: Commit): void {
    const key = this.branchIdKey(commit.branchId);
    let branch = this.branches.get(key);

    if (!branch) {
      branch = {
        branchId: commit.branchId,
        commits: new Set([commit.hash]),
        firstCommitHash: commit.hash,
        firstCommitDate: commit.date,
        lastCommitHash: commit.hash,
        lastCommitDate: commit.date,
        mergeCommitHash: null,
        mergeCommitDate: null,
        targetBranch: null,
        mergedParentHash: null,
        isCurrent: commit.isOnCurrentBranch
      };
      this.branches.set(key, branch);
    } else {
      branch.commits.add(commit.hash);

      if (commit.date < branch.firstCommitDate) {
        branch.firstCommitHash = commit.hash;
        branch.firstCommitDate = commit.date;
      }
      if (commit.date > branch.lastCommitDate) {
        branch.lastCommitHash = commit.hash;
        branch.lastCommitDate = commit.date;
      }
    }
  }

  private processMerge(mergeCommit: Commit): void {
    // Process all parents except the first (which is the current branch)
    for (let i = 1; i < mergeCommit.parents.length; i++) {
      const featureBranchHash = mergeCommit.parents[i];
      const featureBranchCommit = this.commitByHash.get(featureBranchHash);

      // Don't mark the current branch as merged when main is merged into a feature branch
      if (featureBranchCommit?.isOnCurrentBranch === true) continue;

      if (featureBranchCommit) {
        const featureBranchKey = this.branchIdKey(featureBranchCommit.branchId);
        const featureBranch = this.branches.get(featureBranchKey);

        if (featureBranch && featureBranch.mergeCommitHash === null) {
          featureBranch.mergeCommitHash = mergeCommit.hash;
          featureBranch.mergeCommitDate = mergeCommit.date;
          featureBranch.targetBranch = this.getBranchName(mergeCommit.branchId);
          featureBranch.mergedParentHash = featureBranchHash;
        }
      }
    }
  }

  private toBranch(mutable: MutableBranch, currentDate: Date): Branch {
    const wasMerged = mutable.mergeCommitHash !== null;
    const noCommitsAfterMerge = mutable.mergedParentHash
      ? mutable.lastCommitHash === mutable.mergedParentHash
      : false;
    const isCompleted = wasMerged && noCommitsAfterMerge && !mutable.isCurrent;

    const ninetyDaysAgo = new Date(currentDate.getTime() - 90 * 24 * 60 * 60 * 1000);
    const isActive = mutable.lastCommitDate >= ninetyDaysAgo;

    let status: BranchStatus;
    if (isCompleted) {
      status = 'Completed';
    } else if (isActive) {
      status = 'Active';
    } else {
      status = 'Stale';
    }

    return {
      branchId: mutable.branchId,
      commits: Array.from(mutable.commits),
      firstCommitHash: mutable.firstCommitHash,
      firstCommitDate: mutable.firstCommitDate,
      lastCommitHash: mutable.lastCommitHash,
      lastCommitDate: mutable.lastCommitDate,
      mergeCommitHash: mutable.mergeCommitHash,
      mergeDate: mutable.mergeCommitDate,
      targetBranch: mutable.targetBranch,
      isCurrent: mutable.isCurrent,
      status
    };
  }

  private branchIdKey(branchId: BranchId): string {
    if (branchId.type === 'nameless') {
      return `nameless:${branchId.tipCommit}`;
    }
    return `${branchId.type}:${branchId.name}`;
  }

  private getBranchName(branchId: BranchId): string | null {
    if (branchId.type === 'certain' || branchId.type === 'inferred') {
      return branchId.name;
    }
    return null;
  }
}
