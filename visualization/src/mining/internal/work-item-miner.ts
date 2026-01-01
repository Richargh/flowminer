import type {Author, Commit, CommitType, WorkKey} from '../api-types/commit.ts';
import {type AuthorContribution, type WorkItem, WorkItems} from "../api-types/work-items.ts";

interface MutableWorkItem {
  workKey: WorkKey;
  linesAdded: number;
  linesRemoved: number;
  firstCommitDate: Date;
  lastCommitDate: Date;
  filesChanged: Set<string>;
  fileTouchCount: Map<string, number>;
  absoluteChurnByAuthor: Map<string, number>;
  absoluteChurnByType: Map<CommitType, number>;
  commits: number;
  collaborators: Set<string>;
}

export class WorkItemMiner {
  private readonly workItems: Map<string, MutableWorkItem> = new Map();

  process(commit: Commit): void {
    let workKeys = commit.workKeys;
    if (workKeys.length === 0) {
      workKeys = [{ type: 'unknown' }];
    }

    for (const workKey of workKeys) {
      const key = this.workKeyToString(workKey);
      const item = this.getOrCreate(key, workKey, commit.date);
      this.addCommitToItem(item, commit);
    }
  }

  getResult(): WorkItems {
    const items = Array.from(this.workItems.values()).map(m => this.toWorkItem(m));
    return new WorkItems(items);
  }

  private getOrCreate(key: string, workKey: WorkKey, date: Date): MutableWorkItem {
    let item = this.workItems.get(key);
    if (!item) {
      item = {
        workKey,
        linesAdded: 0,
        linesRemoved: 0,
        firstCommitDate: date,
        lastCommitDate: date,
        filesChanged: new Set(),
        fileTouchCount: new Map(),
        absoluteChurnByAuthor: new Map(),
        absoluteChurnByType: new Map(),
        commits: 0,
        collaborators: new Set()
      };
      this.workItems.set(key, item);
    }
    return item;
  }

  private addCommitToItem(item: MutableWorkItem, commit: Commit): void {
    item.commits++;
    item.collaborators.add(this.authorKey(commit.author));
    for (const coAuthor of commit.coAuthors) {
      item.collaborators.add(this.authorKey(coAuthor));
    }

    let commitLinesChanged = 0;
    for (const fc of commit.fileChanges) {
      item.linesAdded += fc.additions;
      item.linesRemoved += fc.deletions;
      item.filesChanged.add(fc.path);
      item.fileTouchCount.set(fc.path, (item.fileTouchCount.get(fc.path) ?? 0) + 1);
      commitLinesChanged += fc.additions + fc.deletions;
    }

    const authorKey = this.authorKey(commit.author);
    item.absoluteChurnByAuthor.set(
      authorKey,
      (item.absoluteChurnByAuthor.get(authorKey) ?? 0) + commitLinesChanged
    );
    item.absoluteChurnByType.set(
      commit.commitType,
      (item.absoluteChurnByType.get(commit.commitType) ?? 0) + commitLinesChanged
    );

    if (commit.date < item.firstCommitDate) {
      item.firstCommitDate = commit.date;
    }
    if (commit.date > item.lastCommitDate) {
      item.lastCommitDate = commit.date;
    }
  }

  private toWorkItem(m: MutableWorkItem): WorkItem {
    const contributions: AuthorContribution[] = [];
    for (const [authorKey, lines] of m.absoluteChurnByAuthor) {
      const [name, email] = authorKey.split(':');
      contributions.push({ author: { name, email }, linesChanged: lines });
    }
    contributions.sort((a, b) => b.linesChanged - a.linesChanged);

    const reworkFiles: string[] = [];
    for (const [path, count] of m.fileTouchCount) {
      if (count > 1) {
        reworkFiles.push(path);
      }
    }

    return {
      workKey: m.workKey,
      linesAdded: m.linesAdded,
      linesRemoved: m.linesRemoved,
      firstCommitDate: m.firstCommitDate,
      lastCommitDate: m.lastCommitDate,
      filesChanged: Array.from(m.filesChanged),
      contributions,
      absoluteChurnByType: m.absoluteChurnByType,
      commits: m.commits,
      collaborators: m.collaborators.size,
      reworkFiles
    };
  }

  private workKeyToString(key: WorkKey): string {
    return key.type === 'known' ? key.key : '(unknown)';
  }

  private authorKey(author: Author): string {
    return `${author.name}:${author.email}`;
  }
}
