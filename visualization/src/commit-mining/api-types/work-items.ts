import type {Author, CommitType, WorkKey} from "../../commit/api-types/commit.ts";

export interface AuthorContribution {
    author: Author;
    linesChanged: number;
}

export interface WorkItem {
    workKey: WorkKey;
    linesAdded: number;
    linesRemoved: number;
    firstCommitDate: Date;
    lastCommitDate: Date;
    filesChanged: string[];
    contributions: AuthorContribution[];
    absoluteChurnByType: Map<CommitType, number>;
    commits: number;
    collaborators: number;
    reworkFiles: string[];
}

export class WorkItems {
    private readonly items: Map<string, WorkItem>;

    constructor(items: WorkItem[]) {
        this.items = new Map(items.map(i => [this.workKeyToString(i.workKey), i]));
    }

    all(): WorkItem[] {
        return Array.from(this.items.values());
    }

    size(): number {
        return this.items.size;
    }

    get(workKey: WorkKey): WorkItem | undefined {
        return this.items.get(this.workKeyToString(workKey));
    }

    private workKeyToString(key: WorkKey): string {
        return key.type === 'known' ? key.key : '(unknown)';
    }
}