import type { Author, Commit, CommitType, BranchId, WorkKey } from '../api-types/commit.ts';
import type {SerializedCommitDto} from "./serializable-commit-dto.ts";

export function toCommit(dto: SerializedCommitDto): Commit {
  return {
    hash: dto.hash,
    author: {
      name: dto.authorName,
      email: dto.authorEmail
    },
    date: new Date(dto.date).getTime(),
    message: dto.message,
    parents: dto.parents,
    fileChanges: dto.fileChanges.map(fc => ({
      path: fc.path,
      additions: fc.additions,
      deletions: fc.deletions,
      isRename: fc.isRename,
      oldPath: fc.oldPath ?? null
    })),
    coAuthors: extractCoAuthors(dto.message),
    commitType: mapCommitType(dto.commitType),
    workKeys: dto.workKeys.map(mapWorkKey),
    branchId: mapBranchId(dto.branchId),
    isOnCurrentBranch: dto.isOnCurrentBranch,
    isMerge: dto.isMerge
  };
}

const CO_AUTHOR_REGEX = /^Co-authored-by:\s*(.+?)\s*<([^>]+)>\s*$/gim;

function extractCoAuthors(message: string): Author[] {
  const coAuthors: Author[] = [];
  let match;
  while ((match = CO_AUTHOR_REGEX.exec(message)) !== null) {
    coAuthors.push({
      name: match[1].trim(),
      email: match[2].trim()
    });
  }
  return coAuthors;
}

function mapCommitType(type: string): CommitType {
  const upperType = type.toUpperCase();
  if (['FEATURE', 'FIX', 'REFACTOR', 'TEST', 'DOCS', 'ENVIRONMENT'].includes(upperType)) {
    return upperType as CommitType;
  }
  return 'UNKNOWN';
}

function mapWorkKey(key: string | null): WorkKey {
  if (key === null) {
    return { type: 'unknown' };
  }
  return { type: 'known', key };
}

function mapBranchId(branchId: { type: string; name?: string | null; tipCommit?: string | null }): BranchId {
  switch (branchId.type) {
    case 'certain':
      return { type: 'certain', name: branchId.name! };
    case 'inferred':
      return { type: 'inferred', name: branchId.name! };
    case 'nameless':
      return { type: 'nameless', tipCommit: branchId.tipCommit! };
    default:
      return { type: 'certain', name: branchId.name ?? 'unknown' };
  }
}
