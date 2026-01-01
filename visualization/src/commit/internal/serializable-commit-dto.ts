import type {BranchIdDto, CommitDto, FileChangeDto} from "teamcharta-git-importer";

/**
 * Type utility to convert KMP types to their JSON-serialized form.
 * Removes the __doNotUseOrImplementIt brand and converts KtList to arrays.
 */
type Unbrand<T> = Omit<T, '__doNotUseOrImplementIt'>;
/**
 * Serialized form of FileChangeDto (from JSON.parse).
 * Derives from KMP's FileChangeDto but removes the brand symbol.
 */
export type SerializedFileChangeDto = Unbrand<FileChangeDto>;
/**
 * Serialized form of BranchIdDto (from JSON.parse).
 * Derives from KMP's BranchIdDto but removes the brand symbol.
 */
export type SerializedBranchIdDto = Unbrand<BranchIdDto>;
/**
 * Serialized form of CommitDto (from JSON.parse).
 * Derives from KMP's CommitDto but:
 * - Removes the brand symbol
 * - Converts KtList<T> to T[] (what JSON.parse produces)
 */
export type SerializedCommitDto = Unbrand<{
    hash: CommitDto['hash'];
    authorName: CommitDto['authorName'];
    authorEmail: CommitDto['authorEmail'];
    date: CommitDto['date'];
    message: CommitDto['message'];
    parents: string[];
    branchId: SerializedBranchIdDto;
    workKeys: (string | null)[];
    commitType: CommitDto['commitType'];
    fileChanges: SerializedFileChangeDto[];
    isOnCurrentBranch: CommitDto['isOnCurrentBranch'];
    isMerge: CommitDto['isMerge'];
}>;