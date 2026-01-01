import type {BranchId} from "../../commit/api-types/commit.ts";

export type BranchStatus = 'Active' | 'Stale' | 'Completed';

export interface Branch {
    branchId: BranchId;
    commits: string[];
    firstCommitHash: string;
    firstCommitDate: Date;
    lastCommitHash: string;
    lastCommitDate: Date;
    mergeCommitHash: string | null;
    mergeDate: Date | null;
    targetBranch: string | null;
    isCurrent: boolean;
    status: BranchStatus;
}

export class Branches {
    private readonly namedBranches: Map<string, Branch>;
    readonly unnamed: Branch[];

    constructor(branches: Branch[]) {
        this.namedBranches = new Map();
        this.unnamed = [];

        for (const branch of branches) {
            const name = this.getBranchName(branch.branchId);
            if (name) {
                this.namedBranches.set(name, branch);
            } else {
                this.unnamed.push(branch);
            }
        }
    }

    get(name: string): Branch | undefined {
        return this.namedBranches.get(name);
    }

    all(): Branch[] {
        return [...this.namedBranches.values(), ...this.unnamed];
    }

    size(): number {
        return this.namedBranches.size + this.unnamed.length;
    }

    private getBranchName(branchId: BranchId): string | null {
        if (branchId.type === 'certain' || branchId.type === 'inferred') {
            return branchId.name;
        }
        return null;
    }
}