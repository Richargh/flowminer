import { describe, it, expect } from 'vitest';
import { parseJsonl, type CommitDto } from './jsonl-parser';

describe('JsonlParser', () => {
  it('parses a single JSONL line into a CommitDto', () => {
    const jsonlLine = '{"hash":"abc123","authorName":"Alice","authorEmail":"alice@example.com","date":"2024-01-01T10:00:00Z","message":"Initial commit","parents":[],"branchId":{"type":"certain","name":"main","tipCommit":null},"workKeys":[],"commitType":"UNKNOWN","fileChanges":[],"isOnCurrentBranch":true,"isMerge":false}';

    const commits = parseJsonl(jsonlLine);

    expect(commits).toHaveLength(1);
    expect(commits[0].hash).toBe('abc123');
    expect(commits[0].authorName).toBe('Alice');
    expect(commits[0].authorEmail).toBe('alice@example.com');
  });

  it('parses multiple JSONL lines into CommitDto array', () => {
    const jsonlContent = `{"hash":"abc123","authorName":"Alice","authorEmail":"alice@example.com","date":"2024-01-01T10:00:00Z","message":"First","parents":[],"branchId":{"type":"certain","name":"main","tipCommit":null},"workKeys":[],"commitType":"UNKNOWN","fileChanges":[],"isOnCurrentBranch":true,"isMerge":false}
{"hash":"def456","authorName":"Bob","authorEmail":"bob@example.com","date":"2024-01-02T10:00:00Z","message":"Second","parents":["abc123"],"branchId":{"type":"certain","name":"main","tipCommit":null},"workKeys":[],"commitType":"UNKNOWN","fileChanges":[],"isOnCurrentBranch":true,"isMerge":false}`;

    const commits = parseJsonl(jsonlContent);

    expect(commits).toHaveLength(2);
    expect(commits[0].hash).toBe('abc123');
    expect(commits[1].hash).toBe('def456');
    expect(commits[1].parents).toEqual(['abc123']);
  });

  it('parses commits with file changes', () => {
    const jsonlLine = '{"hash":"abc123","authorName":"Alice","authorEmail":"alice@example.com","date":"2024-01-01T10:00:00Z","message":"Add file","parents":[],"branchId":{"type":"certain","name":"main","tipCommit":null},"workKeys":[],"commitType":"UNKNOWN","fileChanges":[{"path":"src/main.ts","additions":10,"deletions":5,"isRename":false,"oldPath":null}],"isOnCurrentBranch":true,"isMerge":false}';

    const commits = parseJsonl(jsonlLine);

    expect(commits[0].fileChanges).toHaveLength(1);
    expect(commits[0].fileChanges[0].path).toBe('src/main.ts');
    expect(commits[0].fileChanges[0].additions).toBe(10);
    expect(commits[0].fileChanges[0].deletions).toBe(5);
  });

  it('parses commits with work keys', () => {
    const jsonlLine = '{"hash":"abc123","authorName":"Alice","authorEmail":"alice@example.com","date":"2024-01-01T10:00:00Z","message":"FEAT-123: Add feature","parents":[],"branchId":{"type":"certain","name":"main","tipCommit":null},"workKeys":["FEAT-123",null],"commitType":"UNKNOWN","fileChanges":[],"isOnCurrentBranch":true,"isMerge":false}';

    const commits = parseJsonl(jsonlLine);

    expect(commits[0].workKeys).toEqual(['FEAT-123', null]);
  });

  it('parses different branch types', () => {
    const certainBranch = '{"hash":"a","authorName":"A","authorEmail":"a@a.com","date":"2024-01-01T10:00:00Z","message":"m","parents":[],"branchId":{"type":"certain","name":"main","tipCommit":null},"workKeys":[],"commitType":"UNKNOWN","fileChanges":[],"isOnCurrentBranch":true,"isMerge":false}';
    const inferredBranch = '{"hash":"b","authorName":"B","authorEmail":"b@b.com","date":"2024-01-01T10:00:00Z","message":"m","parents":[],"branchId":{"type":"inferred","name":"feature","tipCommit":null},"workKeys":[],"commitType":"UNKNOWN","fileChanges":[],"isOnCurrentBranch":false,"isMerge":false}';
    const namelessBranch = '{"hash":"c","authorName":"C","authorEmail":"c@c.com","date":"2024-01-01T10:00:00Z","message":"m","parents":[],"branchId":{"type":"nameless","name":null,"tipCommit":"xyz789"},"workKeys":[],"commitType":"UNKNOWN","fileChanges":[],"isOnCurrentBranch":false,"isMerge":false}';

    const certain = parseJsonl(certainBranch)[0];
    const inferred = parseJsonl(inferredBranch)[0];
    const nameless = parseJsonl(namelessBranch)[0];

    expect(certain.branchId.type).toBe('certain');
    expect(certain.branchId.name).toBe('main');
    expect(inferred.branchId.type).toBe('inferred');
    expect(nameless.branchId.type).toBe('nameless');
    expect(nameless.branchId.tipCommit).toBe('xyz789');
  });

  it('skips empty lines', () => {
    const jsonlContent = `{"hash":"abc123","authorName":"Alice","authorEmail":"alice@example.com","date":"2024-01-01T10:00:00Z","message":"First","parents":[],"branchId":{"type":"certain","name":"main","tipCommit":null},"workKeys":[],"commitType":"UNKNOWN","fileChanges":[],"isOnCurrentBranch":true,"isMerge":false}

{"hash":"def456","authorName":"Bob","authorEmail":"bob@example.com","date":"2024-01-02T10:00:00Z","message":"Second","parents":[],"branchId":{"type":"certain","name":"main","tipCommit":null},"workKeys":[],"commitType":"UNKNOWN","fileChanges":[],"isOnCurrentBranch":true,"isMerge":false}`;

    const commits = parseJsonl(jsonlContent);

    expect(commits).toHaveLength(2);
  });

  it('returns empty array for empty input', () => {
    expect(parseJsonl('')).toEqual([]);
    expect(parseJsonl('   ')).toEqual([]);
  });
});
