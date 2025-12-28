export interface FileChangeDto {
  path: string;
  additions: number;
  deletions: number;
  isRename: boolean;
  oldPath: string | null;
}

export interface BranchIdDto {
  type: 'certain' | 'inferred' | 'nameless';
  name: string | null;
  tipCommit: string | null;
}

export interface CommitDto {
  hash: string;
  authorName: string;
  authorEmail: string;
  date: string;
  message: string;
  parents: string[];
  branchId: BranchIdDto;
  workKeys: (string | null)[];
  commitType: string;
  fileChanges: FileChangeDto[];
  isOnCurrentBranch: boolean;
  isMerge: boolean;
}

export function parseJsonl(content: string): CommitDto[] {
  if (!content.trim()) {
    return [];
  }

  return content
    .split('\n')
    .filter(line => line.trim().length > 0)
    .map(line => JSON.parse(line) as CommitDto);
}
