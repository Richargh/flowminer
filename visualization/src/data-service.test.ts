import { describe, it, expect } from 'vitest';
import { loadVisualizationData, type VisualizationData } from './data-service';

describe('DataService', () => {
  it('loads and parses visualization data from JSON', async () => {
    const mockJsonData = {
      authors: [
        { name: 'Alice', commitCount: 100, linesAdded: 5000, linesDeleted: 2000, avgCommitSize: 70.0 },
        { name: 'Bob', commitCount: 50, linesAdded: 2500, linesDeleted: 1000, avgCommitSize: 70.0 }
      ],
      commitTimeline: [
        { date: '2024-01-01', cumulativeCount: 10, author: 'Alice' },
        { date: '2024-01-02', cumulativeCount: 15, author: 'Bob' }
      ],
      workItems: [
        { key: 'TASK-1', type: 'Feature', startDate: '2024-01-01', durationDays: 5.0 },
        { key: 'TASK-2', type: 'Bug', startDate: '2024-01-03', durationDays: 2.0 }
      ]
    };

    const data: VisualizationData = loadVisualizationData(mockJsonData);

    expect(data.authors).toHaveLength(2);
    expect(data.authors[0].name).toBe('Alice');
    expect(data.authors[0].commitCount).toBe(100);

    expect(data.commitTimeline).toHaveLength(2);
    expect(data.commitTimeline[0].date).toBe('2024-01-01');

    expect(data.workItems).toHaveLength(2);
    expect(data.workItems[0].key).toBe('TASK-1');
    expect(data.workItems[0].durationDays).toBe(5.0);
  });

  it('returns empty arrays for missing data', () => {
    const emptyData = loadVisualizationData({});

    expect(emptyData.authors).toEqual([]);
    expect(emptyData.commitTimeline).toEqual([]);
    expect(emptyData.workItems).toEqual([]);
  });
});
