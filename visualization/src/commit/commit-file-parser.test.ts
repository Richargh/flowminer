import { describe, it, expect } from 'vitest';
import { parseLine, parseJsonlLines } from './commit-file-parser.ts';
import type {SerializedCommitDto} from "./internal/serializable-commit-dto.ts";
import {createCommitDtoJson} from "./__fixtures__/commit-dto-builder.ts";

describe('jsonl-parser', () => {
  describe('parseLine', () => {
    it('returns null for empty string', () => {
      expect(parseLine('')).toBeNull();
    });

    it('returns null for whitespace-only string', () => {
      expect(parseLine('   ')).toBeNull();
      expect(parseLine('\t')).toBeNull();
      expect(parseLine('\n')).toBeNull();
    });

    it('deserializes valid JSON to CommitDto', () => {
      const json = createCommitDtoJson({
        message: 'Initial commit',
        branchId: { type: 'certain', name: 'main', tipCommit: 'abc123' },
        workKeys: ['TASK-1'],
        fileChanges: [{ path: 'file.ts', additions: 10, deletions: 5, isRename: false, oldPath: null }]
      });

      const result = parseLine(json) as SerializedCommitDto;

      expect(result).not.toBeNull();
      expect(result.hash).toBe('abc123');
      expect(result.authorName).toBe('Alice');
      expect(result.authorEmail).toBe('alice@example.com');
      expect(result.date).toBe('2024-01-15T10:30:00Z');
      expect(result.message).toBe('Initial commit');
      expect(result.parents).toEqual([]);
      expect(result.branchId.type).toBe('certain');
      expect(result.branchId.name).toBe('main');
      expect(result.workKeys).toEqual(['TASK-1']);
      expect(result.commitType).toBe('Feature');
      expect(result.fileChanges).toHaveLength(1);
      expect(result.fileChanges[0].path).toBe('file.ts');
      expect(result.isOnCurrentBranch).toBe(true);
      expect(result.isMerge).toBe(false);
    });
  });

  describe('parseJsonlLines', () => {
    it('yields CommitDto for each valid line', () => {
      const jsonl = [
        createCommitDtoJson({ hash: 'abc123', authorName: 'Alice' }),
        createCommitDtoJson({ hash: 'def456', authorName: 'Bob' }),
        createCommitDtoJson({ hash: 'ghi789', authorName: 'Carol' })
      ].join('\n');

      const commits = Array.from(parseJsonlLines(jsonl));

      expect(commits).toHaveLength(3);
      expect(commits[0].hash).toBe('abc123');
      expect(commits[0].authorName).toBe('Alice');
      expect(commits[1].hash).toBe('def456');
      expect(commits[1].authorName).toBe('Bob');
      expect(commits[2].hash).toBe('ghi789');
      expect(commits[2].authorName).toBe('Carol');
    });

    it('skips empty lines', () => {
      const jsonl = [
        createCommitDtoJson({ hash: 'abc123', authorName: 'Alice' }),
        '',
        '   ',
        createCommitDtoJson({ hash: 'def456', authorName: 'Bob' })
      ].join('\n');

      const commits = Array.from(parseJsonlLines(jsonl));

      expect(commits).toHaveLength(2);
      expect(commits[0].hash).toBe('abc123');
      expect(commits[1].hash).toBe('def456');
    });
  });
});
