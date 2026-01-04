import type {Commit} from '../api-types/commit.ts';

export const createCommit = (overrides: Partial<Commit> = {}): Commit => ({
  hash: 'abc123',
  author: { name: 'Alice', email: 'alice@example.com' },
  date: new Date('2024-01-15').getTime(),
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

