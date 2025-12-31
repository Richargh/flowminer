import type { Commit } from '../domain';
import type { SerializedCommitDto } from '../jsonl-parser';

export const createCommit = (overrides: Partial<Commit> = {}): Commit => ({
  hash: 'abc123',
  author: { name: 'Alice', email: 'alice@example.com' },
  date: new Date('2024-01-15'),
  message: 'Test commit',
  parents: [],
  fileChanges: [],
  coAuthors: [],
  commitType: 'FEATURE',
  workKeys: [],
  branchId: { type: 'certain', name: 'main' },
  isOnCurrentBranch: true,
  isMerge: false,
  ...overrides
});

export const createCommitDto = (overrides: Partial<SerializedCommitDto> = {}): SerializedCommitDto => ({
  hash: 'abc123',
  authorName: 'Alice',
  authorEmail: 'alice@example.com',
  date: '2024-01-15T10:30:00Z',
  message: 'Test commit',
  parents: [],
  branchId: { type: 'certain', name: 'main', tipCommit: null },
  workKeys: [],
  commitType: 'Feature',
  fileChanges: [],
  isOnCurrentBranch: true,
  isMerge: false,
  ...overrides
});

export const createCommitDtoJson = (overrides: Partial<SerializedCommitDto> = {}): string =>
  JSON.stringify(createCommitDto(overrides));
