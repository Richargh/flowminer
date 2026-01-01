import type {Author, Commit, CommitType} from '../../commit/api-types/commit.ts';
import {type AuthorStatistic, AuthorStatistics, type ChurnMetric} from "../api-types/author.ts";

interface MutableAuthor {
  author: Author;
  commitCount: number;
  linesAdded: number;
  linesRemoved: number;
  workItems: Set<string>;
  churnByCommitType: Map<CommitType, ChurnMetric>;
  directCoAuthors: Set<string>;
}

export class AuthorMiner {
  private readonly authorData: Map<string, MutableAuthor> = new Map();
  private readonly workKeyToAuthors: Map<string, Set<string>> = new Map();

  process(commit: Commit): void {
    const authorKey = this.authorKey(commit.author);
    const accumulator = this.getOrCreateAccumulator(authorKey, commit.author);

    // Update commit count
    accumulator.commitCount++;

    // Calculate churn for this commit
    const additions = commit.fileChanges.reduce((sum, fc) => sum + fc.additions, 0);
    const deletions = commit.fileChanges.reduce((sum, fc) => sum + fc.deletions, 0);
    accumulator.linesAdded += additions;
    accumulator.linesRemoved += deletions;

    // Update churn by commit type
    const existingChurn = accumulator.churnByCommitType.get(commit.commitType) ?? { additions: 0, deletions: 0 };
    accumulator.churnByCommitType.set(commit.commitType, {
      additions: existingChurn.additions + additions,
      deletions: existingChurn.deletions + deletions
    });

    // Track work keys
    for (const workKey of commit.workKeys) {
      if (workKey.type === 'known') {
        accumulator.workItems.add(workKey.key);

        // Track work key to author mapping
        let authorsForKey = this.workKeyToAuthors.get(workKey.key);
        if (!authorsForKey) {
          authorsForKey = new Set();
          this.workKeyToAuthors.set(workKey.key, authorsForKey);
        }
        authorsForKey.add(authorKey);
      }
    }

    // Track direct co-authors
    for (const coAuthor of commit.coAuthors) {
      accumulator.directCoAuthors.add(this.authorKey(coAuthor));
    }
  }

  getResult(): AuthorStatistics {
    const stats: AuthorStatistic[] = [];

    for (const [authorKey, acc] of this.authorData) {
      // Find work item collaborators
      const workItemCollaborators = new Set<string>();
      for (const workKey of acc.workItems) {
        const authorsForKey = this.workKeyToAuthors.get(workKey);
        if (authorsForKey) {
          for (const otherAuthorKey of authorsForKey) {
            if (otherAuthorKey !== authorKey) {
              workItemCollaborators.add(otherAuthorKey);
            }
          }
        }
      }

      // Combine direct co-authors and work item collaborators
      const allCollaboratorKeys = new Set([...acc.directCoAuthors, ...workItemCollaborators]);
      const collaborators: Author[] = [];
      for (const collabKey of allCollaboratorKeys) {
        const collabAcc = this.authorData.get(collabKey);
        if (collabAcc) {
          collaborators.push(collabAcc.author);
        } else {
          // Co-author who hasn't made any commits themselves
          const [name, email] = collabKey.split(':');
          collaborators.push({ name, email });
        }
      }

      stats.push({
        author: acc.author,
        commitCount: acc.commitCount,
        linesAdded: acc.linesAdded,
        linesRemoved: acc.linesRemoved,
        workItems: Array.from(acc.workItems).map(key => ({ type: 'known' as const, key })),
        churnByCommitType: acc.churnByCommitType,
        collaborators
      });
    }

    return new AuthorStatistics(stats);
  }

  private getOrCreateAccumulator(key: string, author: Author): MutableAuthor {
    let acc = this.authorData.get(key);
    if (!acc) {
      acc = {
        author,
        commitCount: 0,
        linesAdded: 0,
        linesRemoved: 0,
        workItems: new Set(),
        churnByCommitType: new Map(),
        directCoAuthors: new Set()
      };
      this.authorData.set(key, acc);
    }
    return acc;
  }

  private authorKey(author: Author): string {
    return `${author.name}:${author.email}`;
  }
}
