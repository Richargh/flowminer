import type { GitMiningResult } from './app/api-types/git-mining-result.ts';
import { WorkItems } from './app/api-types/work-items.ts';
import { filterWorkItemsByRange } from './filter-work-items.ts';

export interface TimeRange {
  startDate: string;
  endDate: string;
}

export class GitMiningCoordinator {
  private fullResult: GitMiningResult | null = null;
  private currentRange: TimeRange | null = null;

  storeResult(result: GitMiningResult): void {
    this.fullResult = result;
  }

  setRange(range: TimeRange): void {
    this.currentRange = range;
    this.dispatchDataConstrained();
  }

  private dispatchDataConstrained(): void {
    if (!this.fullResult || !this.currentRange) return;

    const filteredWorkItems = filterWorkItemsByRange(
      this.fullResult.workItems.all(),
      this.currentRange.startDate,
      this.currentRange.endDate
    );

    const filteredResult: GitMiningResult = {
      ...this.fullResult,
      workItems: new WorkItems(filteredWorkItems)
    };

    document.dispatchEvent(new CustomEvent<GitMiningResult>('data-constrained', {
      detail: filteredResult,
      bubbles: true,
      composed: true
    }));
  }

  getFullResult(): GitMiningResult | null {
    return this.fullResult;
  }

  getCurrentRange(): TimeRange | null {
    return this.currentRange;
  }
}
