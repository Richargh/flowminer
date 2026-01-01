import type {Commit} from '../commit/api-types/commit.ts';
import {AuthorMiner} from './internal/author-miner.ts';
import {WorkItemMiner} from './internal/work-item-miner.ts';
import {BranchMiner} from './internal/branch-miner.ts';
import {Commits, type GitMiningResult} from "./api-types/git-mining-result.ts";

export class CommitMiner {
  private readonly authorMiner = new AuthorMiner();
  private readonly workItemMiner = new WorkItemMiner();
  private readonly branchMiner = new BranchMiner();
  private readonly processedCommits: Commit[] = [];

  process(commit: Commit): void {
    this.processedCommits.push(commit);
    this.authorMiner.process(commit);
    this.workItemMiner.process(commit);
    this.branchMiner.process(commit);
  }

  getResult(currentDate: Date): GitMiningResult {
    return {
      commits: new Commits(this.processedCommits),
      branches: this.branchMiner.getResult(currentDate),
      workItems: this.workItemMiner.getResult(),
      authorStatistics: this.authorMiner.getResult()
    };
  }
}
