import type {Author, CommitType, WorkKey} from "../../commit/api-types/commit.ts";

export interface ChurnMetric {
    additions: number;
    deletions: number;
}

export interface AuthorStatistic {
    author: Author;
    commitCount: number;
    linesAdded: number;
    linesRemoved: number;
    workItems: WorkKey[];
    churnByCommitType: Map<CommitType, ChurnMetric>;
    collaborators: Author[];
}

export class AuthorStatistics {
    private readonly authorStatistics: Map<string, AuthorStatistic>;

    constructor(stats: AuthorStatistic[]) {
        this.authorStatistics = new Map(
            stats.map(s => [this.authorKey(s.author), s])
        );
    }

    all(): AuthorStatistic[] {
        return Array.from(this.authorStatistics.values());
    }

    size(): number {
        return this.authorStatistics.size;
    }

    get(author: Author): AuthorStatistic | undefined {
        return this.authorStatistics.get(this.authorKey(author));
    }

    private authorKey(author: Author): string {
        return `${author.name}:${author.email}`;
    }
}