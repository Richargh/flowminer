import './commit-range/commit-range-panel.ts';
import './work-item-scatter/work-item-scatter-panel.ts';
import './work-item-duration-histogram/work-item-duration-panel.ts';
import './theme-switcher/theme-switcher';
import './file-loading/file-loader.ts';
import type { WorkItemDuration, HistogramBucket } from './data-service';
import {aggregateCommitsByDate, type CommitActivity} from './commit-range/app/internal/commit-activity.ts';

import type {GitMiningResult} from "./commit-mining/app/api-types/git-mining-result.ts";

const sampleWorkItems: WorkItemDuration[] = [
  { key: 'FEAT-1', type: 'Feature', startDate: '2024-01-05', durationDays: 5 },
  { key: 'BUG-1', type: 'Bug', startDate: '2024-01-10', durationDays: 2 },
  { key: 'FEAT-2', type: 'Feature', startDate: '2024-01-15', durationDays: 8 },
  { key: 'BUG-2', type: 'Bug', startDate: '2024-01-20', durationDays: 1 },
  { key: 'FEAT-3', type: 'Feature', startDate: '2024-01-28', durationDays: 12 },
  { key: 'BUG-3', type: 'Bug', startDate: '2024-02-05', durationDays: 3 },
  { key: 'FEAT-4', type: 'Feature', startDate: '2024-02-10', durationDays: 6 },
  { key: 'BUG-4', type: 'Bug', startDate: '2024-02-15', durationDays: 4 },
];

const sampleHistogram: HistogramBucket[] = [
  { range: '0-2 days', count: 15, minValue: 0, maxValue: 2 },
  { range: '2-4 days', count: 28, minValue: 2, maxValue: 4 },
  { range: '4-6 days', count: 22, minValue: 4, maxValue: 6 },
  { range: '6-8 days', count: 12, minValue: 6, maxValue: 8 },
  { range: '8-10 days', count: 8, minValue: 8, maxValue: 10 },
  { range: '10+ days', count: 5, minValue: 10, maxValue: 999 },
];

const sampleCommitActivity: CommitActivity[] = [
  { date: '2024-01-01', count: 5 },
  { date: '2024-01-02', count: 8 },
  { date: '2024-01-03', count: 3 },
  { date: '2024-01-04', count: 12 },
  { date: '2024-01-05', count: 7 },
  { date: '2024-01-08', count: 10 },
  { date: '2024-01-09', count: 6 },
  { date: '2024-01-10', count: 15 },
  { date: '2024-01-11', count: 4 },
  { date: '2024-01-12', count: 9 },
];

const activityPanel = document.querySelector('commit-range-panel');
if (activityPanel) {
  activityPanel.data = sampleCommitActivity;
}

const scatterPanel = document.querySelector('work-item-scatter-panel');
if (scatterPanel) {
  scatterPanel.workItems = sampleWorkItems;
}

const histogramPanel = document.querySelector('work-item-duration-panel');
if (histogramPanel) {
  histogramPanel.buckets = sampleHistogram;
}

// Listen for loaded JSONL data
document.addEventListener('data-loaded', ((event: CustomEvent<GitMiningResult>) => {
  const result = event.detail;

  if (activityPanel) {
    activityPanel.data = aggregateCommitsByDate(result.commits.all());
  }

  if (scatterPanel) {
    scatterPanel.workItems = result.workItems.all()
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
  }
}) as EventListener);
