import './commit-range/commit-range-panel.ts';
import './work-item-scatter/work-item-scatter-panel.ts';
import './work-item-duration-histogram/work-item-duration-panel.ts';
import './theme-switcher/theme-switcher';
import './file-loading/file-loader.ts';
import {aggregateCommitsByDate} from './commit-range/app/internal/commit-activity.ts';
import { loadDefaultData } from './startup/default-data-loader.ts';

import type {GitMiningResult} from "./commit-mining/app/api-types/git-mining-result.ts";
import type {HistogramBucket} from "./data-service.ts";

const activityPanel = document.querySelector('commit-range-panel');
const scatterPanel = document.querySelector('work-item-scatter-panel');
const histogramPanel = document.querySelector('work-item-duration-panel');

function computeHistogramBuckets(durations: number[]): HistogramBucket[] {
  const bucketRanges = [
    { range: '0-2 days', minValue: 0, maxValue: 2 },
    { range: '2-4 days', minValue: 2, maxValue: 4 },
    { range: '4-6 days', minValue: 4, maxValue: 6 },
    { range: '6-8 days', minValue: 6, maxValue: 8 },
    { range: '8-10 days', minValue: 8, maxValue: 10 },
    { range: '10+ days', minValue: 10, maxValue: 999 },
  ];

  return bucketRanges.map(bucket => ({
    ...bucket,
    count: durations.filter(d => d >= bucket.minValue && d < bucket.maxValue).length
  }));
}

function updatePanels(result: GitMiningResult): void {
  if (activityPanel) {
    activityPanel.data = aggregateCommitsByDate(result.commits.all());
  }

  const workItemData = result.workItems.all()
    .filter(wi => wi.workKey.type === 'known')
    .map(wi => {
      const durationMs = wi.lastCommitDate.getTime() - wi.firstCommitDate.getTime();
      const durationDays = Math.max(1, Math.round(durationMs / (1000 * 60 * 60 * 24)));
      let maxType = 'Feature';
      let maxChurn = 0;
      for (const [type, churn] of wi.absoluteChurnByType) {
        if (churn > maxChurn) {
          maxChurn = churn;
          maxType = type;
        }
      }
      return {
        key: (wi.workKey as { type: 'known'; key: string }).key,
        type: maxType,
        startDate: wi.firstCommitDate.toISOString().split('T')[0],
        durationDays
      };
    });

  if (scatterPanel) {
    scatterPanel.workItems = workItemData;
  }

  if (histogramPanel) {
    histogramPanel.buckets = computeHistogramBuckets(workItemData.map(wi => wi.durationDays));
  }
}

// Listen for loaded JSONL data
document.addEventListener('data-loaded', ((event: CustomEvent<GitMiningResult>) => {
  updatePanels(event.detail);
}) as EventListener);

// Load default data at startup via the same event path as file loading
document.dispatchEvent(new CustomEvent<GitMiningResult>('data-loaded', {
  detail: loadDefaultData(),
  bubbles: true,
  composed: true
}));
