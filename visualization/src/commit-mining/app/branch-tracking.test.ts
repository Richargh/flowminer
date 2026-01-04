import { describe, it, expect, beforeEach } from 'vitest';
import { CommitMiner } from './commit-miner.ts';
import { createCommit } from '../../commit/app/__fixtures__/commit-builder.ts';

describe('Branch Tracking', () => {
  let miner: CommitMiner;

  beforeEach(() => {
    miner = new CommitMiner();
  });

  describe('commit grouping', () => {
    it('groups commits by branch', () => {
      miner.process(createCommit({ hash: 'main1', branchId: { type: 'certain', name: 'main' } }));
      miner.process(createCommit({ hash: 'feat1', branchId: { type: 'certain', name: 'feature/test' } }));
      miner.process(createCommit({ hash: 'main2', branchId: { type: 'certain', name: 'main' } }));

      const result = miner.getResult(Date.now());

      expect(result.branches.all()).toHaveLength(2);
      expect(result.branches.get('main')?.commits).toHaveLength(2);
      expect(result.branches.get('feature/test')?.commits).toHaveLength(1);
    });
  });

  describe('date tracking', () => {
    it('tracks first and last commit dates per branch', () => {
      miner.process(createCommit({
        hash: 'a',
        branchId: { type: 'certain', name: 'main' },
        date: new Date('2024-01-15').getTime()
      }));
      miner.process(createCommit({
        hash: 'b',
        branchId: { type: 'certain', name: 'main' },
        date: new Date('2024-01-10').getTime()
      }));
      miner.process(createCommit({
        hash: 'c',
        branchId: { type: 'certain', name: 'main' },
        date: new Date('2024-01-20').getTime()
      }));

      const result = miner.getResult(Date.now());
      const branch = result.branches.get('main');

      expect(branch?.firstCommitDate).toBe(new Date('2024-01-10').getTime());
      expect(branch?.lastCommitDate).toBe(new Date('2024-01-20').getTime());
    });
  });

  describe('merge detection', () => {
    it('detects merge commits and tracks target branch', () => {
      miner.process(createCommit({
        hash: 'feature1',
        branchId: { type: 'certain', name: 'feature/test' },
        parents: ['main1'],
        isOnCurrentBranch: false
      }));

      miner.process(createCommit({
        hash: 'merge1',
        branchId: { type: 'certain', name: 'main' },
        parents: ['main1', 'feature1'],
        isMerge: true,
        isOnCurrentBranch: true
      }));

      const result = miner.getResult(Date.now());
      const featureBranch = result.branches.get('feature/test');

      expect(featureBranch?.mergeCommitHash).toBe('merge1');
      expect(featureBranch?.targetBranch).toBe('main');
    });
  });

  describe('branch status classification', () => {
    it('classifies active branches (recent activity, not merged)', () => {
      const now = new Date('2024-03-15').getTime();
      const within90Days = new Date('2024-01-20').getTime();

      miner.process(createCommit({
        branchId: { type: 'certain', name: 'feature/active' },
        date: within90Days,
        isOnCurrentBranch: false
      }));

      const result = miner.getResult(now);
      const branch = result.branches.get('feature/active');

      expect(branch?.status).toBe('Active');
    });

    it('classifies stale branches (no recent activity, not merged)', () => {
      const now = new Date('2024-06-15').getTime();
      const olderThan90Days = new Date('2024-01-15').getTime();

      miner.process(createCommit({
        branchId: { type: 'certain', name: 'feature/stale' },
        date: olderThan90Days,
        isOnCurrentBranch: false
      }));

      const result = miner.getResult(now);
      const branch = result.branches.get('feature/stale');

      expect(branch?.status).toBe('Stale');
    });

    it('classifies completed branches (merged with no subsequent commits)', () => {
      const now = new Date('2024-03-15').getTime();

      miner.process(createCommit({
        hash: 'feature1',
        branchId: { type: 'certain', name: 'feature/done' },
        parents: [],
        date: new Date('2024-01-10').getTime(),
        isOnCurrentBranch: false
      }));

      miner.process(createCommit({
        hash: 'merge1',
        branchId: { type: 'certain', name: 'main' },
        parents: ['main1', 'feature1'],
        date: new Date('2024-01-15').getTime(),
        isMerge: true,
        isOnCurrentBranch: true
      }));

      const result = miner.getResult(now);
      const branch = result.branches.get('feature/done');

      expect(branch?.status).toBe('Completed');
    });
  });
});
