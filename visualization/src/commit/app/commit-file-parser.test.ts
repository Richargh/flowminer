import { describe, it, expect } from 'vitest';
import { parseJsonlLines } from './commit-file-parser.ts';
import {createCommitDtoJson} from "./__fixtures__/commit-dto-builder.ts";

describe('jsonl-parser', () => {
  describe('parseJsonlLines', () => {
    it('parses valid JSON line to Commit', () => {
      const jsonl = createCommitDtoJson({
        message: 'Initial commit',
        branchId: { type: 'certain', name: 'main', tipCommit: 'abc123' },
        workKeys: ['TASK-1'],
        fileChanges: [{ path: 'file.ts', additions: 10, deletions: 5, isRename: false, oldPath: null }]
      });

      const commits = Array.from(parseJsonlLines(jsonl));

      expect(commits).toHaveLength(1);
      const result = commits[0];
      expect(result.hash).toBe('abc123');
      expect(result.author.name).toBe('Alice');
      expect(result.author.email).toBe('alice@example.com');
      expect(result.date).toBe(1705314600000);
      expect(result.message).toBe('Initial commit');
      expect(result.parents).toEqual([]);
      expect(result.branchId.type).toBe('certain');
      expect(result.workKeys).toEqual([{ type: 'known', key: 'TASK-1' }]);
      expect(result.commitType).toBe('FEATURE');
      expect(result.fileChanges).toHaveLength(1);
      expect(result.fileChanges[0].path).toBe('file.ts');
      expect(result.isOnCurrentBranch).toBe(true);
      expect(result.isMerge).toBe(false);
    });

    it('yields Commit for each valid line', () => {
      const jsonl = [
        createCommitDtoJson({ hash: 'abc123', authorName: 'Alice' }),
        createCommitDtoJson({ hash: 'def456', authorName: 'Bob' }),
        createCommitDtoJson({ hash: 'ghi789', authorName: 'Carol' })
      ].join('\n');

      const commits = Array.from(parseJsonlLines(jsonl));

      expect(commits).toHaveLength(3);
      expect(commits[0].hash).toBe('abc123');
      expect(commits[0].author.name).toBe('Alice');
      expect(commits[1].hash).toBe('def456');
      expect(commits[1].author.name).toBe('Bob');
      expect(commits[2].hash).toBe('ghi789');
      expect(commits[2].author.name).toBe('Carol');
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
