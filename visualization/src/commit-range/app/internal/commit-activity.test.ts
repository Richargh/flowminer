import { describe, it, expect } from 'vitest';
import { aggregateCommitsByDate } from './commit-activity.ts';
import { createCommit } from '../../../commit/app/__fixtures__/commit-builder.ts';

describe('Commit Activity Aggregation', () => {
  it('aggregates commits by date', () => {
    const commits = [
      createCommit({ date: new Date('2024-01-15') }),
      createCommit({ date: new Date('2024-01-15') }),
      createCommit({ date: new Date('2024-01-16') }),
    ];

    const activity = aggregateCommitsByDate(commits);

    expect(activity).toHaveLength(2);
    expect(activity[0]).toEqual({ date: '2024-01-15', count: 2 });
    expect(activity[1]).toEqual({ date: '2024-01-16', count: 1 });
  });

  it('returns empty array for no commits', () => {
    const activity = aggregateCommitsByDate([]);

    expect(activity).toEqual([]);
  });

  it('sorts results by date ascending', () => {
    const commits = [
      createCommit({ date: new Date('2024-01-20') }),
      createCommit({ date: new Date('2024-01-10') }),
      createCommit({ date: new Date('2024-01-15') }),
    ];

    const activity = aggregateCommitsByDate(commits);

    expect(activity[0].date).toBe('2024-01-10');
    expect(activity[1].date).toBe('2024-01-15');
    expect(activity[2].date).toBe('2024-01-20');
  });
});
