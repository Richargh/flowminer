import { describe, it, expect, beforeEach } from 'vitest';
import { CommitMiner } from './commit-miner.ts';
import { createCommit } from '../../commit/app/__fixtures__/commit-builder.ts';

describe('Git Mining', () => {
  let miner: CommitMiner;

  beforeEach(() => {
    miner = new CommitMiner();
  });

  describe('commit collection', () => {
    it('collects all processed commits', () => {
      const commit1 = createCommit({ hash: 'abc123' });
      const commit2 = createCommit({ hash: 'def456' });

      miner.process(commit1);
      miner.process(commit2);

      const result = miner.getResult(Date.now());

      expect(result.commits.all()).toHaveLength(2);
      expect(result.commits.get(0).hash).toBe('abc123');
      expect(result.commits.get(1).hash).toBe('def456');
    });
  });

  describe('result aggregation', () => {
    it('provides author statistics, work items, and branches in the result', () => {
      miner.process(createCommit({
        hash: 'commit1',
        author: { name: 'Alice', email: 'alice@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }],
        branchId: { type: 'certain', name: 'main' }
      }));
      miner.process(createCommit({
        hash: 'commit2',
        author: { name: 'Bob', email: 'bob@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-2' }],
        branchId: { type: 'certain', name: 'feature/test' }
      }));

      const result = miner.getResult(Date.now());

      expect(result.commits.all()).toHaveLength(2);
      expect(result.authorStatistics.size()).toBe(2);
      expect(result.workItems.size()).toBe(2);
      expect(result.branches.size()).toBe(2);
    });
  });
});
