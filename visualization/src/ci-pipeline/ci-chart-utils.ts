export const MAX_LABEL_LENGTH = 30;

export function truncate(name: string, maxLength: number): string {
  return name.length > maxLength ? name.slice(0, maxLength) + '…' : name;
}

export function formatDuration(seconds: number): string {
  return seconds >= 60 ? `${(seconds / 60).toFixed(1)}m` : `${seconds.toFixed(1)}s`;
}
