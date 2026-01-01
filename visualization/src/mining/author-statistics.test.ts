import { describe, it, expect, beforeEach } from 'vitest';
import { GitMiner } from './git-miner.ts';
import { createCommit } from './__fixtures__/commit-builder.ts';

describe('Author Statistics', () => {
  let miner: GitMiner;

  beforeEach(() => {
    miner = new GitMiner();
  });

  describe('commit tracking', () => {
    it('counts commits per author', () => {
      miner.process(createCommit({ author: { name: 'Alice', email: 'alice@example.com' } }));
      miner.process(createCommit({ author: { name: 'Alice', email: 'alice@example.com' } }));
      miner.process(createCommit({ author: { name: 'Bob', email: 'bob@example.com' } }));

      const result = miner.getResult(new Date());

      expect(result.authorStatistics.all()).toHaveLength(2);
      expect(result.authorStatistics.get({ name: 'Alice', email: 'alice@example.com' })?.commitCount).toBe(2);
      expect(result.authorStatistics.get({ name: 'Bob', email: 'bob@example.com' })?.commitCount).toBe(1);
    });
  });

  describe('line changes', () => {
    it('accumulates lines added and removed per author', () => {
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        fileChanges: [
          { path: 'file1.ts', additions: 10, deletions: 5, isRename: false, oldPath: null },
          { path: 'file2.ts', additions: 20, deletions: 10, isRename: false, oldPath: null }
        ]
      }));
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        fileChanges: [
          { path: 'file3.ts', additions: 5, deletions: 2, isRename: false, oldPath: null }
        ]
      }));

      const result = miner.getResult(new Date());
      const aliceStats = result.authorStatistics.get({ name: 'Alice', email: 'alice@example.com' });

      expect(aliceStats?.linesAdded).toBe(35);
      expect(aliceStats?.linesRemoved).toBe(17);
    });
  });

  describe('churn by commit type', () => {
    it('tracks churn separately for each commit type', () => {
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        commitType: 'FEATURE',
        fileChanges: [{ path: 'a.ts', additions: 10, deletions: 5, isRename: false, oldPath: null }]
      }));
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        commitType: 'FIX',
        fileChanges: [{ path: 'b.ts', additions: 3, deletions: 2, isRename: false, oldPath: null }]
      }));
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        commitType: 'FEATURE',
        fileChanges: [{ path: 'c.ts', additions: 7, deletions: 1, isRename: false, oldPath: null }]
      }));

      const result = miner.getResult(new Date());
      const aliceStats = result.authorStatistics.get({ name: 'Alice', email: 'alice@example.com' });

      expect(aliceStats?.churnByCommitType.get('FEATURE')).toEqual({ additions: 17, deletions: 6 });
      expect(aliceStats?.churnByCommitType.get('FIX')).toEqual({ additions: 3, deletions: 2 });
    });
  });

  describe('collaboration tracking', () => {
    it('tracks direct co-authors from commits', () => {
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        coAuthors: [
          { name: 'Bob', email: 'bob@example.com' },
          { name: 'Carol', email: 'carol@example.com' }
        ]
      }));

      const result = miner.getResult(new Date());
      const aliceStats = result.authorStatistics.get({ name: 'Alice', email: 'alice@example.com' });

      expect(aliceStats?.collaborators).toHaveLength(2);
      expect(aliceStats?.collaborators).toContainEqual({ name: 'Bob', email: 'bob@example.com' });
      expect(aliceStats?.collaborators).toContainEqual({ name: 'Carol', email: 'carol@example.com' });
    });

    it('detects collaborators through shared work items', () => {
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }]
      }));
      miner.process(createCommit({
        author: { name: 'Bob', email: 'bob@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }]
      }));

      const result = miner.getResult(new Date());
      const aliceStats = result.authorStatistics.get({ name: 'Alice', email: 'alice@example.com' });
      const bobStats = result.authorStatistics.get({ name: 'Bob', email: 'bob@example.com' });

      expect(aliceStats?.collaborators).toContainEqual({ name: 'Bob', email: 'bob@example.com' });
      expect(bobStats?.collaborators).toContainEqual({ name: 'Alice', email: 'alice@example.com' });
    });
  });

  describe('work item tracking', () => {
    it('tracks unique work items per author', () => {
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }, { type: 'known', key: 'TASK-2' }]
      }));
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }, { type: 'known', key: 'TASK-3' }]
      }));

      const result = miner.getResult(new Date());
      const aliceStats = result.authorStatistics.get({ name: 'Alice', email: 'alice@example.com' });

      expect(aliceStats?.workItems).toHaveLength(3);
      expect(aliceStats?.workItems).toContainEqual({ type: 'known', key: 'TASK-1' });
      expect(aliceStats?.workItems).toContainEqual({ type: 'known', key: 'TASK-2' });
      expect(aliceStats?.workItems).toContainEqual({ type: 'known', key: 'TASK-3' });
    });
  });
});
