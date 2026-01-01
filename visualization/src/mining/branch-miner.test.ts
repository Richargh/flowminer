import { describe, it, expect, beforeEach } from 'vitest';
import { BranchMiner } from './internal/branch-miner.ts';
import { createCommit } from './__fixtures__/commit-builder.ts';

describe('BranchMiner', () => {
  let miner: BranchMiner;

  beforeEach(() => {
    miner = new BranchMiner();
  });

  describe('process', () => {
    it('groups commits by branchId', () => {
      miner.process(createCommit({ hash: 'main1', branchId: { type: 'certain', name: 'main' } }));
      miner.process(createCommit({ hash: 'feat1', branchId: { type: 'certain', name: 'feature/test' } }));
      miner.process(createCommit({ hash: 'main2', branchId: { type: 'certain', name: 'main' } }));

      const result = miner.getResult(new Date());

      expect(result.all()).toHaveLength(2);
      expect(result.get('main')?.commits).toHaveLength(2);
      expect(result.get('feature/test')?.commits).toHaveLength(1);
    });

    it('tracks first and last commit dates per branch', () => {
      miner.process(createCommit({
        hash: 'a',
        branchId: { type: 'certain', name: 'main' },
        date: new Date('2024-01-15')
      }));
      miner.process(createCommit({
        hash: 'b',
        branchId: { type: 'certain', name: 'main' },
        date: new Date('2024-01-10')
      }));
      miner.process(createCommit({
        hash: 'c',
        branchId: { type: 'certain', name: 'main' },
        date: new Date('2024-01-20')
      }));

      const result = miner.getResult(new Date());
      const branch = result.get('main');

      expect(branch?.firstCommitDate.toISOString()).toBe('2024-01-10T00:00:00.000Z');
      expect(branch?.lastCommitDate.toISOString()).toBe('2024-01-20T00:00:00.000Z');
    });

    it('detects merge commits and tracks target branch', () => {
      // Feature branch commit
      miner.process(createCommit({
        hash: 'feature1',
        branchId: { type: 'certain', name: 'feature/test' },
        parents: ['main1'],
        isOnCurrentBranch: false
      }));

      // Merge commit on main (merging feature/test)
      miner.process(createCommit({
        hash: 'merge1',
        branchId: { type: 'certain', name: 'main' },
        parents: ['main1', 'feature1'],
        isMerge: true,
        isOnCurrentBranch: true
      }));

      const result = miner.getResult(new Date());
      const featureBranch = result.get('feature/test');

      expect(featureBranch?.mergeCommitHash).toBe('merge1');
      expect(featureBranch?.targetBranch).toBe('main');
    });

    it('classifies Active branches (last commit within 90 days, not merged)', () => {
      const now = new Date('2024-03-15');
      const within90Days = new Date('2024-01-20');

      miner.process(createCommit({
        branchId: { type: 'certain', name: 'feature/active' },
        date: within90Days,
        isOnCurrentBranch: false
      }));

      const result = miner.getResult(now);
      const branch = result.get('feature/active');

      expect(branch?.status).toBe('Active');
    });

    it('classifies Stale branches (last commit older than 90 days, not merged)', () => {
      const now = new Date('2024-06-15');
      const olderThan90Days = new Date('2024-01-15');

      miner.process(createCommit({
        branchId: { type: 'certain', name: 'feature/stale' },
        date: olderThan90Days,
        isOnCurrentBranch: false
      }));

      const result = miner.getResult(now);
      const branch = result.get('feature/stale');

      expect(branch?.status).toBe('Stale');
    });

    it('classifies Completed branches (merged with no subsequent commits)', () => {
      const now = new Date('2024-03-15');

      // Feature branch commit
      miner.process(createCommit({
        hash: 'feature1',
        branchId: { type: 'certain', name: 'feature/done' },
        parents: [],
        date: new Date('2024-01-10'),
        isOnCurrentBranch: false
      }));

      // Merge commit on main
      miner.process(createCommit({
        hash: 'merge1',
        branchId: { type: 'certain', name: 'main' },
        parents: ['main1', 'feature1'],
        date: new Date('2024-01-15'),
        isMerge: true,
        isOnCurrentBranch: true
      }));

      const result = miner.getResult(now);
      const branch = result.get('feature/done');

      expect(branch?.status).toBe('Completed');
    });
  });
});
