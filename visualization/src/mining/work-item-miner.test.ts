import { describe, it, expect, beforeEach } from 'vitest';
import { WorkItemMiner } from './internal/work-item-miner.ts';
import { createCommit } from './__fixtures__/commit-builder.ts';

describe('WorkItemMiner', () => {
  let miner: WorkItemMiner;

  beforeEach(() => {
    miner = new WorkItemMiner();
  });

  describe('process', () => {
    it('groups commits by work key', () => {
      miner.process(createCommit({ workKeys: [{ type: 'known', key: 'TASK-1' }] }));
      miner.process(createCommit({ workKeys: [{ type: 'known', key: 'TASK-2' }] }));
      miner.process(createCommit({ workKeys: [{ type: 'known', key: 'TASK-1' }] }));

      const result = miner.getResult();

      expect(result.all()).toHaveLength(2);
      expect(result.get({ type: 'known', key: 'TASK-1' })?.commits).toBe(2);
      expect(result.get({ type: 'known', key: 'TASK-2' })?.commits).toBe(1);
    });

    it('handles commits with unknown work key', () => {
      miner.process(createCommit({ workKeys: [{ type: 'unknown' }] }));
      miner.process(createCommit({ workKeys: [] }));

      const result = miner.getResult();

      expect(result.all()).toHaveLength(1);
      expect(result.get({ type: 'unknown' })?.commits).toBe(2);
    });

    it('tracks first and last commit dates per work item', () => {
      miner.process(createCommit({
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        date: new Date('2024-01-15')
      }));
      miner.process(createCommit({
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        date: new Date('2024-01-10')
      }));
      miner.process(createCommit({
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        date: new Date('2024-01-20')
      }));

      const result = miner.getResult();
      const workItem = result.get({ type: 'known', key: 'TASK-1' });

      expect(workItem?.firstCommitDate.toISOString()).toBe('2024-01-10T00:00:00.000Z');
      expect(workItem?.lastCommitDate.toISOString()).toBe('2024-01-20T00:00:00.000Z');
    });

    it('accumulates lines added and removed per work item', () => {
      miner.process(createCommit({
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        fileChanges: [
          { path: 'a.ts', additions: 10, deletions: 5, isRename: false, oldPath: null }
        ]
      }));
      miner.process(createCommit({
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        fileChanges: [
          { path: 'b.ts', additions: 20, deletions: 10, isRename: false, oldPath: null }
        ]
      }));

      const result = miner.getResult();
      const workItem = result.get({ type: 'known', key: 'TASK-1' });

      expect(workItem?.linesAdded).toBe(30);
      expect(workItem?.linesRemoved).toBe(15);
    });

    it('tracks files changed per work item', () => {
      miner.process(createCommit({
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        fileChanges: [
          { path: 'a.ts', additions: 10, deletions: 0, isRename: false, oldPath: null },
          { path: 'b.ts', additions: 5, deletions: 0, isRename: false, oldPath: null }
        ]
      }));
      miner.process(createCommit({
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        fileChanges: [
          { path: 'a.ts', additions: 3, deletions: 0, isRename: false, oldPath: null }
        ]
      }));

      const result = miner.getResult();
      const workItem = result.get({ type: 'known', key: 'TASK-1' });

      expect(workItem?.filesChanged).toContain('a.ts');
      expect(workItem?.filesChanged).toContain('b.ts');
      expect(workItem?.filesChanged).toHaveLength(2);
    });

    it('identifies rework files (touched more than once)', () => {
      miner.process(createCommit({
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        fileChanges: [
          { path: 'a.ts', additions: 10, deletions: 0, isRename: false, oldPath: null },
          { path: 'b.ts', additions: 5, deletions: 0, isRename: false, oldPath: null }
        ]
      }));
      miner.process(createCommit({
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        fileChanges: [
          { path: 'a.ts', additions: 3, deletions: 0, isRename: false, oldPath: null }
        ]
      }));

      const result = miner.getResult();
      const workItem = result.get({ type: 'known', key: 'TASK-1' });

      expect(workItem?.reworkFiles).toContain('a.ts');
      expect(workItem?.reworkFiles).not.toContain('b.ts');
    });

    it('tracks author contributions per work item', () => {
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        fileChanges: [{ path: 'a.ts', additions: 10, deletions: 5, isRename: false, oldPath: null }]
      }));
      miner.process(createCommit({
        author: { name: 'Bob', email: 'bob@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        fileChanges: [{ path: 'b.ts', additions: 30, deletions: 0, isRename: false, oldPath: null }]
      }));

      const result = miner.getResult();
      const workItem = result.get({ type: 'known', key: 'TASK-1' });

      expect(workItem?.contributions).toHaveLength(2);
      // Sorted by lines changed descending
      expect(workItem?.contributions[0].author.name).toBe('Bob');
      expect(workItem?.contributions[0].linesChanged).toBe(30);
      expect(workItem?.contributions[1].author.name).toBe('Alice');
      expect(workItem?.contributions[1].linesChanged).toBe(15);
    });

    it('tracks collaborators count per work item', () => {
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        coAuthors: [{ name: 'Bob', email: 'bob@example.com' }]
      }));
      miner.process(createCommit({
        author: { name: 'Carol', email: 'carol@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }]
      }));

      const result = miner.getResult();
      const workItem = result.get({ type: 'known', key: 'TASK-1' });

      expect(workItem?.collaborators).toBe(3); // Alice, Bob, Carol
    });
  });
});
