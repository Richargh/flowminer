import type {Commit} from "./domain.ts";
import {Branches} from "./internal/branch-miner.ts";
import {WorkItems} from "./internal/work-item-miner.ts";
import {AuthorStatistics} from "./internal/author-miner.ts";

export class Commits {
    private readonly commits: Commit[];

    constructor(commits: Commit[]) {
        this.commits = commits;
    }

    get(index: number): Commit {
        return this.commits[index];
    }

    all(): Commit[] {
        return this.commits;
    }

    first(): Commit {
        return this.commits[0];
    }

    size(): number {
        return this.commits.length;
    }
}

export interface GitMiningResult {
    commits: Commits;
    branches: Branches;
    workItems: WorkItems;
    authorStatistics: AuthorStatistics;
}