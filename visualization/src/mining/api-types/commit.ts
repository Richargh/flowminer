export interface Author {
  name: string;
  email: string;
}

export interface FileChange {
  path: string;
  additions: number;
  deletions: number;
  isRename: boolean;
  oldPath: string | null;
}

export type CommitType = 'FEATURE' | 'FIX' | 'REFACTOR' | 'TEST' | 'DOCS' | 'ENVIRONMENT' | 'UNKNOWN';

export type WorkKey =
  | { type: 'known'; key: string }
  | { type: 'unknown' };

export type BranchId =
  | { type: 'certain'; name: string }
  | { type: 'inferred'; name: string }
  | { type: 'nameless'; tipCommit: string };

export interface Commit {
  hash: string;
  author: Author;
  date: Date;
  message: string;
  parents: string[];
  fileChanges: FileChange[];
  coAuthors: Author[];
  commitType: CommitType;
  workKeys: WorkKey[];
  branchId: BranchId;
  isOnCurrentBranch: boolean;
  isMerge: boolean;
}
