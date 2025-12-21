import './components/author-radar-panel';
import './components/commit-timeline-panel';
import './components/work-item-scatter-panel';
import type { AuthorRadarPanel } from './components/author-radar-panel';
import type { CommitTimelinePanel } from './components/commit-timeline-panel';
import type { WorkItemScatterPanel } from './components/work-item-scatter-panel';
import type { AuthorStats, CommitTimeline, WorkItemDuration } from './data-service';

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

const app = document.getElementById('app');
if (app) {
  app.innerHTML = `
    <div class="container mx-auto p-4">
      <h1 class="text-2xl font-bold mb-6">Git Visualization</h1>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="card bg-base-100 shadow-xl">
          <div class="card-body">
            <h2 class="card-title">Author Statistics</h2>
            <author-radar-panel id="radar-panel"></author-radar-panel>
          </div>
        </div>

        <div class="card bg-base-100 shadow-xl">
          <div class="card-body">
            <h2 class="card-title">Commit Timeline</h2>
            <commit-timeline-panel id="timeline-panel"></commit-timeline-panel>
          </div>
        </div>

        <div class="card bg-base-100 shadow-xl lg:col-span-2">
          <div class="card-body">
            <h2 class="card-title">Work Item Duration</h2>
            <work-item-scatter-panel id="scatter-panel"></work-item-scatter-panel>
          </div>
        </div>
      </div>
    </div>
  `;

  const radarPanel = document.getElementById('radar-panel') as AuthorRadarPanel;
  if (radarPanel) {
    radarPanel.authors = sampleAuthors;
  }

  const timelinePanel = document.getElementById('timeline-panel') as CommitTimelinePanel;
  if (timelinePanel) {
    timelinePanel.timeline = sampleTimeline;
  }

  const scatterPanel = document.getElementById('scatter-panel') as WorkItemScatterPanel;
  if (scatterPanel) {
    scatterPanel.workItems = sampleWorkItems;
  }
}
