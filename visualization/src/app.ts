import './components/author-radar-panel';
import './components/commit-timeline-panel';
import './components/work-item-scatter-panel';
import './components/sankey-panel';
import './components/histogram-panel';
import './components/theme-switcher';
import './components/file-loader';
import type { AuthorStats, CommitTimeline, WorkItemDuration, SankeyFlow, HistogramBucket } from './data-service';

import type {GitMiningResult} from "./commit-mining/api-types/git-mining-result.ts";

const sampleAuthors: AuthorStats[] = [
  { name: 'Alice', commitCount: 120, linesAdded: 8500, linesDeleted: 3200, avgCommitSize: 97 },
  { name: 'Bob', commitCount: 85, linesAdded: 4200, linesDeleted: 1800, avgCommitSize: 71 },
  { name: 'Charlie', commitCount: 65, linesAdded: 3100, linesDeleted: 1400, avgCommitSize: 69 },
  { name: 'Diana', commitCount: 150, linesAdded: 9800, linesDeleted: 4100, avgCommitSize: 93 },
];

const sampleTimeline: CommitTimeline[] = [
  { date: '2024-01-01', cumulativeCount: 10, author: 'Alice' },
  { date: '2024-01-08', cumulativeCount: 25, author: 'Bob' },
  { date: '2024-01-15', cumulativeCount: 45, author: 'Alice' },
  { date: '2024-01-22', cumulativeCount: 68, author: 'Charlie' },
  { date: '2024-01-29', cumulativeCount: 92, author: 'Diana' },
  { date: '2024-02-05', cumulativeCount: 120, author: 'Alice' },
  { date: '2024-02-12', cumulativeCount: 148, author: 'Bob' },
  { date: '2024-02-19', cumulativeCount: 175, author: 'Diana' },
];

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

const sampleSankeyFlow: SankeyFlow = {
  nodes: [
    { name: 'Alice' },
    { name: 'Bob' },
    { name: 'Charlie' },
    { name: 'Diana' },
    { name: 'Frontend' },
    { name: 'Backend' },
    { name: 'Infrastructure' },
    { name: 'Database' },
    { name: 'Cache' },
    { name: 'API Gateway' },
    { name: 'Monitoring' },
  ],
  links: [
    { source: 'Alice', target: 'Frontend', value: 45 },
    { source: 'Alice', target: 'Backend', value: 30 },
    { source: 'Bob', target: 'Backend', value: 50 },
    { source: 'Bob', target: 'Infrastructure', value: 25 },
    { source: 'Charlie', target: 'Frontend', value: 35 },
    { source: 'Charlie', target: 'Backend', value: 20 },
    { source: 'Diana', target: 'Backend', value: 40 },
    { source: 'Diana', target: 'Infrastructure', value: 55 },
    { source: 'Frontend', target: 'API Gateway', value: 60 },
    { source: 'Frontend', target: 'Cache', value: 20 },
    { source: 'Backend', target: 'Database', value: 80 },
    { source: 'Backend', target: 'Cache', value: 40 },
    { source: 'Backend', target: 'API Gateway', value: 20 },
    { source: 'Infrastructure', target: 'Monitoring', value: 50 },
    { source: 'Infrastructure', target: 'Database', value: 30 },
  ],
};

const sampleHistogram: HistogramBucket[] = [
  { range: '0-2 days', count: 15, minValue: 0, maxValue: 2 },
  { range: '2-4 days', count: 28, minValue: 2, maxValue: 4 },
  { range: '4-6 days', count: 22, minValue: 4, maxValue: 6 },
  { range: '6-8 days', count: 12, minValue: 6, maxValue: 8 },
  { range: '8-10 days', count: 8, minValue: 8, maxValue: 10 },
  { range: '10+ days', count: 5, minValue: 10, maxValue: 999 },
];

const radarPanel = document.querySelector('author-radar-panel');
if (radarPanel) {
  radarPanel.authors = sampleAuthors;
}

const timelinePanel = document.querySelector('commit-timeline-panel');
if (timelinePanel) {
  timelinePanel.timeline = sampleTimeline;
}

const scatterPanel = document.querySelector('work-item-scatter-panel');
if (scatterPanel) {
  scatterPanel.workItems = sampleWorkItems;
}

const sankeyPanel = document.querySelector('sankey-panel');
if (sankeyPanel) {
  sankeyPanel.flow = sampleSankeyFlow;
}

const histogramPanel = document.querySelector('histogram-panel');
if (histogramPanel) {
  histogramPanel.buckets = sampleHistogram;
}

// Listen for loaded JSONL data
document.addEventListener('data-loaded', ((event: CustomEvent<GitMiningResult>) => {
  const result = event.detail;

  if (radarPanel) {
    radarPanel.authors = result.authorStatistics.all().map(stat => ({
      name: stat.author.name,
      commitCount: stat.commitCount,
      linesAdded: stat.linesAdded,
      linesDeleted: stat.linesRemoved,
      avgCommitSize: stat.commitCount > 0
        ? Math.round((stat.linesAdded + stat.linesRemoved) / stat.commitCount)
        : 0
    }));
  }

  if (timelinePanel) {
    const commitsByDate = new Map<string, { author: string; count: number }[]>();
    for (const commit of result.commits.all()) {
      const dateKey = commit.date.toISOString().split('T')[0];
      if (!commitsByDate.has(dateKey)) {
        commitsByDate.set(dateKey, []);
      }
      const existing = commitsByDate.get(dateKey)!.find(e => e.author === commit.author.name);
      if (existing) {
        existing.count++;
      } else {
        commitsByDate.get(dateKey)!.push({ author: commit.author.name, count: 1 });
      }
    }

    const sortedDates = Array.from(commitsByDate.keys()).sort();
    let cumulativeCount = 0;
    const timeline: CommitTimeline[] = [];

    for (const date of sortedDates) {
      for (const { author, count } of commitsByDate.get(date)!) {
        cumulativeCount += count;
        timeline.push({ date, cumulativeCount, author });
      }
    }
    timelinePanel.timeline = timeline;
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
