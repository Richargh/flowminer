import type {SerializedCommitDto} from "../internal/serializable-commit-dto.ts";

export const createCommitDto = (overrides: Partial<SerializedCommitDto> = {}): SerializedCommitDto => ({
    hash: 'abc123',
    authorName: 'Alice',
    authorEmail: 'alice@example.com',
    date: '2024-01-15T10:30:00Z',
    message: 'Test commit',
    parents: [],
    branchId: {type: 'certain', name: 'main', tipCommit: null},
    workKeys: [],
    commitType: 'Feature',
    fileChanges: [],
    isOnCurrentBranch: true,
    isMerge: false,
    ...overrides
});

export const createCommitDtoJson = (overrides: Partial<SerializedCommitDto> = {}): string =>
    JSON.stringify(createCommitDto(overrides));