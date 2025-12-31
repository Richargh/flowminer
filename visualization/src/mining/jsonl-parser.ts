import type {
  CommitDto,
  BranchIdDto,
  FileChangeDto
} from 'teamcharta-git-importer';

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

export function parseLine(line: string): SerializedCommitDto | null {
  if (!line || line.trim() === '') {
    return null;
  }
  return JSON.parse(line) as SerializedCommitDto;
}

export function* parseJsonlLines(content: string): Generator<SerializedCommitDto> {
  const lines = content.split('\n');
  for (const line of lines) {
    const commit = parseLine(line);
    if (commit !== null) {
      yield commit;
    }
  }
}

export async function* parseJsonlStream(stream: ReadableStream): AsyncGenerator<SerializedCommitDto> {
  const textStream = stream.pipeThrough(new TextDecoderStream());
  const reader = textStream.getReader();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += value;
    const lines = buffer.split('\n');
    buffer = lines.pop() ?? '';

    for (const line of lines) {
      const commit = parseLine(line);
      if (commit !== null) {
        yield commit;
      }
    }
  }

  if (buffer.trim()) {
    const commit = parseLine(buffer);
    if (commit !== null) {
      yield commit;
    }
  }
}
