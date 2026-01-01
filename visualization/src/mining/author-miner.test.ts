import { describe, it, expect, beforeEach } from 'vitest';
import { AuthorMiner } from './internal/author-miner.ts';
import { createCommit } from './__fixtures__/commit-builder.ts';

describe('AuthorMiner', () => {
  let miner: AuthorMiner;

  beforeEach(() => {
    miner = new AuthorMiner();
  });

  describe('process', () => {
    it('tracks commit count per author', () => {
      miner.process(createCommit({ author: { name: 'Alice', email: 'alice@example.com' } }));
      miner.process(createCommit({ author: { name: 'Alice', email: 'alice@example.com' } }));
      miner.process(createCommit({ author: { name: 'Bob', email: 'bob@example.com' } }));

      const result = miner.getResult();

      expect(result.all()).toHaveLength(2);
      expect(result.get({ name: 'Alice', email: 'alice@example.com' })?.commitCount).toBe(2);
      expect(result.get({ name: 'Bob', email: 'bob@example.com' })?.commitCount).toBe(1);
    });

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

      const result = miner.getResult();
      const aliceStats = result.get({ name: 'Alice', email: 'alice@example.com' });

      expect(aliceStats?.linesAdded).toBe(35);
      expect(aliceStats?.linesRemoved).toBe(17);
    });

    it('tracks churn by commit type per author', () => {
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

      const result = miner.getResult();
      const aliceStats = result.get({ name: 'Alice', email: 'alice@example.com' });

      expect(aliceStats?.churnByCommitType.get('FEATURE')).toEqual({ additions: 17, deletions: 6 });
      expect(aliceStats?.churnByCommitType.get('FIX')).toEqual({ additions: 3, deletions: 2 });
    });

    it('tracks direct co-authors from commits', () => {
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        coAuthors: [
          { name: 'Bob', email: 'bob@example.com' },
          { name: 'Carol', email: 'carol@example.com' }
        ]
      }));

      const result = miner.getResult();
      const aliceStats = result.get({ name: 'Alice', email: 'alice@example.com' });

      expect(aliceStats?.collaborators).toHaveLength(2);
      expect(aliceStats?.collaborators).toContainEqual({ name: 'Bob', email: 'bob@example.com' });
      expect(aliceStats?.collaborators).toContainEqual({ name: 'Carol', email: 'carol@example.com' });
    });

    it('tracks work keys per author', () => {
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }, { type: 'known', key: 'TASK-2' }]
      }));
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }, { type: 'known', key: 'TASK-3' }]
      }));

      const result = miner.getResult();
      const aliceStats = result.get({ name: 'Alice', email: 'alice@example.com' });

      expect(aliceStats?.workItems).toHaveLength(3);
      expect(aliceStats?.workItems).toContainEqual({ type: 'known', key: 'TASK-1' });
      expect(aliceStats?.workItems).toContainEqual({ type: 'known', key: 'TASK-2' });
      expect(aliceStats?.workItems).toContainEqual({ type: 'known', key: 'TASK-3' });
    });

    it('computes work-item collaborators', () => {
      // Alice and Bob both work on TASK-1
      miner.process(createCommit({
        author: { name: 'Alice', email: 'alice@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }]
      }));
      miner.process(createCommit({
        author: { name: 'Bob', email: 'bob@example.com' },
        workKeys: [{ type: 'known', key: 'TASK-1' }]
      }));

      const result = miner.getResult();
      const aliceStats = result.get({ name: 'Alice', email: 'alice@example.com' });
      const bobStats = result.get({ name: 'Bob', email: 'bob@example.com' });

      expect(aliceStats?.collaborators).toContainEqual({ name: 'Bob', email: 'bob@example.com' });
      expect(bobStats?.collaborators).toContainEqual({ name: 'Alice', email: 'alice@example.com' });
    });
  });
});
