import type {Commit} from "./commit.ts";
import {Branches} from "./branch.ts";
import {AuthorStatistics} from "./author.ts";
import {WorkItems} from "./work-items.ts";

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