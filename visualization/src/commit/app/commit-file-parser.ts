import type {SerializedCommitDto} from "./internal/serializable-commit-dto.ts";
import type {Commit} from "./api-types/commit.ts";
import {toCommit} from "./internal/commit-transform.ts";

function parseLine(line: string): SerializedCommitDto | null {
  if (!line || line.trim() === '') {
    return null;
  }
  return JSON.parse(line) as SerializedCommitDto;
}

export function* parseJsonlLines(content: string): Generator<Commit> {
  const lines = content.split('\n');
  for (const line of lines) {
    const dto = parseLine(line);
    if (dto !== null) {
      yield toCommit(dto);
    }
  }
}

export async function* parseJsonlStream(stream: ReadableStream): AsyncGenerator<Commit> {
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
      const dto = parseLine(line);
      if (dto !== null) {
        yield toCommit(dto);
      }
    }
  }

  if (buffer.trim()) {
    const dto = parseLine(buffer);
    if (dto !== null) {
      yield toCommit(dto);
    }
  }
}
