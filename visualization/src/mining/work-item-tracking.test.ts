import { describe, it, expect, beforeEach } from 'vitest';
import { GitMiner } from './git-miner.ts';
import { createCommit } from '../commit/__fixtures__/commit-builder.ts';

describe('Work Item Tracking', () => {
  let miner: GitMiner;

  beforeEach(() => {
    miner = new GitMiner();
  });

  describe('commit grouping', () => {
    it('groups commits by work key', () => {
      miner.process(createCommit({ workKeys: [{ type: 'known', key: 'TASK-1' }] }));
      miner.process(createCommit({ workKeys: [{ type: 'known', key: 'TASK-2' }] }));
      miner.process(createCommit({ workKeys: [{ type: 'known', key: 'TASK-1' }] }));

      const result = miner.getResult(new Date());

      expect(result.workItems.all()).toHaveLength(2);
      expect(result.workItems.get({ type: 'known', key: 'TASK-1' })?.commits).toBe(2);
      expect(result.workItems.get({ type: 'known', key: 'TASK-2' })?.commits).toBe(1);
    });

    it('handles commits with unknown work key', () => {
      miner.process(createCommit({ workKeys: [{ type: 'unknown' }] }));
      miner.process(createCommit({ workKeys: [] }));

      const result = miner.getResult(new Date());

      expect(result.workItems.all()).toHaveLength(1);
      expect(result.workItems.get({ type: 'unknown' })?.commits).toBe(2);
    });
  });

  describe('date tracking', () => {
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

      const result = miner.getResult(new Date());
      const workItem = result.workItems.get({ type: 'known', key: 'TASK-1' });

      expect(workItem?.firstCommitDate.toISOString()).toBe('2024-01-10T00:00:00.000Z');
      expect(workItem?.lastCommitDate.toISOString()).toBe('2024-01-20T00:00:00.000Z');
    });
  });

  describe('line changes', () => {
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

      const result = miner.getResult(new Date());
      const workItem = result.workItems.get({ type: 'known', key: 'TASK-1' });

      expect(workItem?.linesAdded).toBe(30);
      expect(workItem?.linesRemoved).toBe(15);
    });
  });

  describe('file tracking', () => {
    it('tracks unique files changed per work item', () => {
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

      const result = miner.getResult(new Date());
      const workItem = result.workItems.get({ type: 'known', key: 'TASK-1' });

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

      const result = miner.getResult(new Date());
      const workItem = result.workItems.get({ type: 'known', key: 'TASK-1' });

      expect(workItem?.reworkFiles).toContain('a.ts');
      expect(workItem?.reworkFiles).not.toContain('b.ts');
    });
  });

  describe('author contributions', () => {
    it('tracks contributions per author sorted by lines changed', () => {
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

      const result = miner.getResult(new Date());
      const workItem = result.workItems.get({ type: 'known', key: 'TASK-1' });

      expect(workItem?.contributions).toHaveLength(2);
      expect(workItem?.contributions[0].author.name).toBe('Bob');
      expect(workItem?.contributions[0].linesChanged).toBe(30);
      expect(workItem?.contributions[1].author.name).toBe('Alice');
      expect(workItem?.contributions[1].linesChanged).toBe(15);
    });
  });

  describe('collaboration tracking', () => {
    it('counts unique collaborators per work item', () => {
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        coAuthors: [{ name: 'Bob', email: 'bob@example.com' }]
      }));
      miner.process(createCommit({
        author: { name: 'Carol', email: 'carol@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }]
      }));

      const result = miner.getResult(new Date());
      const workItem = result.workItems.get({ type: 'known', key: 'TASK-1' });

      expect(workItem?.collaborators).toBe(3);
    });
  });
});
