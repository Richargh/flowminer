import { describe, it, expect, beforeEach } from 'vitest';
import { GitMiner } from './git-miner';
import { createCommit } from './__fixtures__/commit-builder.ts';

describe('GitMiner', () => {
  let miner: GitMiner;

  beforeEach(() => {
    miner = new GitMiner();
  });

  describe('process', () => {
    it('delegates to all sub-miners', () => {
      const commit = createCommit({
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        fileChanges: [{ path: 'file.ts', additions: 10, deletions: 5, isRename: false, oldPath: null }]
      });

      miner.process(commit);
      const result = miner.getResult(new Date());

      // AuthorMiner processed the commit
      expect(result.authorStatistics.all()).toHaveLength(1);
      expect(result.authorStatistics.get({ name: 'Alice', email: 'alice@example.com' })?.commitCount).toBe(1);

      // WorkItemMiner processed the commit
      expect(result.workItems.all()).toHaveLength(1);
      expect(result.workItems.get({ type: 'known', key: 'TASK-1' })?.commits).toBe(1);

      // BranchMiner processed the commit
      expect(result.branches.all()).toHaveLength(1);
      expect(result.branches.get('main')?.commits).toHaveLength(1);
    });
  });

  describe('getResult', () => {
    it('returns GitMiningResult with all collected data', () => {
      miner.process(createCommit({
        hash: 'commit1',
        author: { name: 'Alice', email: 'alice@example.com' }
      }));
      miner.process(createCommit({
        hash: 'commit2',
        author: { name: 'Bob', email: 'bob@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-2' }]
      }));

      const result = miner.getResult(new Date());

      expect(result.commits.all()).toHaveLength(2);
      expect(result.authorStatistics.size()).toBe(2);
      expect(result.workItems.size()).toBe(2);
      expect(result.branches.size()).toBe(1);
    });

    it('includes commits collection with processed commits', () => {
      const commit1 = createCommit({ hash: 'abc123' });
      const commit2 = createCommit({ hash: 'def456' });

      miner.process(commit1);
      miner.process(commit2);

      const result = miner.getResult(new Date());

      expect(result.commits.all()).toHaveLength(2);
      expect(result.commits.get(0).hash).toBe('abc123');
      expect(result.commits.get(1).hash).toBe('def456');
    });
  });
});
