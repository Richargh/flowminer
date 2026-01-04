import { describe, it, expect } from 'vitest';
import { toCommit } from './commit-transform.ts';

import type {SerializedCommitDto} from "./serializable-commit-dto.ts";

describe('commit-transform', () => {
  describe('toCommit', () => {
    const createMinimalCommitDto = (): SerializedCommitDto => ({
      hash: 'abc123',
      authorName: 'Alice',
      authorEmail: 'alice@example.com',
      date: '2024-01-15T10:30:00Z',
      message: 'Initial commit',
      parents: [],
      branchId: { type: 'certain', name: 'main', tipCommit: 'abc123' },
      workKeys: [],
      commitType: 'Feature',
      fileChanges: [],
      isOnCurrentBranch: true,
      isMerge: false
    });

    it('transforms basic fields', () => {
      const dto = createMinimalCommitDto();

      const commit = toCommit(dto);

      expect(commit.hash).toBe('abc123');
      expect(commit.author.name).toBe('Alice');
      expect(commit.author.email).toBe('alice@example.com');
      expect(commit.message).toBe('Initial commit');
      expect(commit.isOnCurrentBranch).toBe(true);
      expect(commit.isMerge).toBe(false);
    });

    it('parses ISO date string to milliseconds', () => {
      const dto = createMinimalCommitDto();
      dto.date = '2024-01-15T10:30:00.000Z';

      const commit = toCommit(dto);

      expect(commit.date).toBe(1705314600000);
    });

    it('date supports arithmetic operations', () => {
      const dto = createMinimalCommitDto();
      const commit = toCommit(dto);

      // TypeScript should accept arithmetic on commit.date
      const futureDate = commit.date + 1000;
      expect(typeof futureDate).toBe('number');
    });

    it('converts parents array', () => {
      const dto = createMinimalCommitDto();
      dto.parents = ['parent1', 'parent2'];
      dto.isMerge = true;

      const commit = toCommit(dto);

      expect(commit.parents).toEqual(['parent1', 'parent2']);
      expect(commit.isMerge).toBe(true);
    });

    it('transforms fileChanges', () => {
      const dto = createMinimalCommitDto();
      dto.fileChanges = [
        { path: 'src/index.ts', additions: 10, deletions: 5, isRename: false, oldPath: null },
        { path: 'src/utils.ts', additions: 20, deletions: 0, isRename: true, oldPath: 'src/helpers.ts' }
      ];

      const commit = toCommit(dto);

      expect(commit.fileChanges).toHaveLength(2);
      expect(commit.fileChanges[0].path).toBe('src/index.ts');
      expect(commit.fileChanges[0].additions).toBe(10);
      expect(commit.fileChanges[0].deletions).toBe(5);
      expect(commit.fileChanges[0].isRename).toBe(false);
      expect(commit.fileChanges[1].isRename).toBe(true);
      expect(commit.fileChanges[1].oldPath).toBe('src/helpers.ts');
    });

    it('maps commitType to uppercase enum', () => {
      const dto = createMinimalCommitDto();
      dto.commitType = 'fix';

      const commit = toCommit(dto);

      expect(commit.commitType).toBe('FIX');
    });

    it('maps unknown commitType to UNKNOWN', () => {
      const dto = createMinimalCommitDto();
      dto.commitType = 'random';

      const commit = toCommit(dto);

      expect(commit.commitType).toBe('UNKNOWN');
    });

    it('transforms workKeys with known values', () => {
      const dto = createMinimalCommitDto();
      dto.workKeys = ['TASK-1', 'TASK-2', null];

      const commit = toCommit(dto);

      expect(commit.workKeys).toHaveLength(3);
      expect(commit.workKeys[0]).toEqual({ type: 'known', key: 'TASK-1' });
      expect(commit.workKeys[1]).toEqual({ type: 'known', key: 'TASK-2' });
      expect(commit.workKeys[2]).toEqual({ type: 'unknown' });
    });

    it('transforms branchId certain type', () => {
      const dto = createMinimalCommitDto();
      dto.branchId = { type: 'certain', name: 'feature/test', tipCommit: 'abc123' };

      const commit = toCommit(dto);

      expect(commit.branchId).toEqual({ type: 'certain', name: 'feature/test' });
    });

    it('transforms branchId inferred type', () => {
      const dto = createMinimalCommitDto();
      dto.branchId = { type: 'inferred', name: 'develop', tipCommit: 'def456' };

      const commit = toCommit(dto);

      expect(commit.branchId).toEqual({ type: 'inferred', name: 'develop' });
    });

    it('transforms branchId nameless type', () => {
      const dto = createMinimalCommitDto();
      dto.branchId = { type: 'nameless', name: null, tipCommit: 'ghi789' };

      const commit = toCommit(dto);

      expect(commit.branchId).toEqual({ type: 'nameless', tipCommit: 'ghi789' });
    });

    it('extracts co-authors from commit message trailers', () => {
      const dto = createMinimalCommitDto();
      dto.message = `Add feature

Co-authored-by: Bob <bob@example.com>
Co-authored-by: Carol <carol@example.com>`;

      const commit = toCommit(dto);

      expect(commit.coAuthors).toHaveLength(2);
      expect(commit.coAuthors[0]).toEqual({ name: 'Bob', email: 'bob@example.com' });
      expect(commit.coAuthors[1]).toEqual({ name: 'Carol', email: 'carol@example.com' });
    });

    it('returns empty coAuthors when no trailers present', () => {
      const dto = createMinimalCommitDto();
      dto.message = 'Simple commit message';

      const commit = toCommit(dto);

      expect(commit.coAuthors).toHaveLength(0);
    });
  });
});
