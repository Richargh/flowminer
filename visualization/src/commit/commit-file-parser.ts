import type {SerializedCommitDto} from "./internal/serializable-commit-dto.ts";

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
