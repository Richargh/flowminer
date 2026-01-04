import { describe, it, expect } from 'vitest';
import { filterWorkItemsByRange } from './filter-work-items.ts';
import type { WorkItem } from './app/api-types/work-items.ts';

function createWorkItem(firstCommitDate: number): WorkItem {
  return {
    workKey: { type: 'known', key: `WORK-${firstCommitDate}` },
    linesAdded: 100,
    linesRemoved: 50,
    firstCommitDate,
    lastCommitDate: firstCommitDate,
    filesChanged: ['file.ts'],
    contributions: [],
    absoluteChurnByType: new Map(),
    commits: 1,
    collaborators: 1,
    reworkFiles: []
  };
}

describe('filterWorkItemsByRange', () => {
  it('should filter work items by firstCommitDate within range', () => {
    const workItems = [
      createWorkItem(new Date('2024-01-01').getTime()),
      createWorkItem(new Date('2024-01-05').getTime()),
      createWorkItem(new Date('2024-01-10').getTime())
    ];

    const filtered = filterWorkItemsByRange(workItems, '2024-01-03', '2024-01-08');

    expect(filtered).toHaveLength(1);
    expect(filtered[0].firstCommitDate).toBe(new Date('2024-01-05').getTime());
  });
});
