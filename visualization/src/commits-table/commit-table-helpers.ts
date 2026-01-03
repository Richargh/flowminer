import type { WorkKey, BranchId } from '../commit/app/api-types/commit.ts';

export function isKnownWorkKey(wk: WorkKey): wk is { type: 'known'; key: string } {
  return wk.type === 'known';
}

export function formatWorkKeys(workKeys: WorkKey[]): string {
  return workKeys
    .filter(isKnownWorkKey)
    .map(wk => wk.key)
    .join(', ');
}

export function getBranchName(branchId: BranchId): string {
  if (branchId.type === 'nameless') return '';
  return branchId.name;
}

const SHORT_HASH_LENGTH = 7;
export function formatShortHash(hash: string): string {
  return hash.slice(0, SHORT_HASH_LENGTH);
}

const MAX_MESSAGE_LENGTH = 100;
export function truncateMessage(message: string): string {
  if (message.length <= MAX_MESSAGE_LENGTH) {
    return message;
  }
  return message.slice(0, MAX_MESSAGE_LENGTH) + '…';
}
