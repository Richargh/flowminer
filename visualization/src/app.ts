import './components/author-radar-panel';
import type { AuthorRadarPanel } from './components/author-radar-panel';
import type { AuthorStats } from './data-service';

const sampleAuthors: AuthorStats[] = [
  { name: 'Alice', commitCount: 120, linesAdded: 8500, linesDeleted: 3200, avgCommitSize: 97 },
  { name: 'Bob', commitCount: 85, linesAdded: 4200, linesDeleted: 1800, avgCommitSize: 71 },
  { name: 'Charlie', commitCount: 65, linesAdded: 3100, linesDeleted: 1400, avgCommitSize: 69 },
  { name: 'Diana', commitCount: 150, linesAdded: 9800, linesDeleted: 4100, avgCommitSize: 93 },
];

const app = document.getElementById('app');
if (app) {
  app.innerHTML = `
    <div class="container mx-auto p-4">
      <h1 class="text-2xl font-bold mb-4">Git Visualization</h1>
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title">Author Statistics</h2>
          <author-radar-panel id="radar-panel"></author-radar-panel>
        </div>
      </div>
    </div>
  `;

  const radarPanel = document.getElementById('radar-panel') as AuthorRadarPanel;
  if (radarPanel) {
    radarPanel.authors = sampleAuthors;
  }
}
