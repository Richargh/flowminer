import { describe, it, expect } from 'vitest';
import {
  isKnownWorkKey,
  formatWorkKeys,
  getBranchName,
  formatShortHash,
  truncateMessage
} from './commit-table-helpers.ts';
import type { WorkKey, BranchId } from '../commit/app/api-types/commit.ts';

describe('commit-table-helpers', () => {
  describe('isKnownWorkKey', () => {
    it('returns true for known work keys', () => {
      const knownKey: WorkKey = { type: 'known', key: 'JIRA-123' };
      expect(isKnownWorkKey(knownKey)).toBe(true);
    });

    it('returns false for unknown work keys', () => {
      const unknownKey: WorkKey = { type: 'unknown' };
      expect(isKnownWorkKey(unknownKey)).toBe(false);
    });
  });

  describe('formatWorkKeys', () => {
    it('returns empty string for empty array', () => {
      expect(formatWorkKeys([])).toBe('');
    });

    it('formats single known work key', () => {
      const workKeys: WorkKey[] = [{ type: 'known', key: 'JIRA-123' }];
      expect(formatWorkKeys(workKeys)).toBe('JIRA-123');
    });

    it('formats multiple known work keys with comma separator', () => {
      const workKeys: WorkKey[] = [
        { type: 'known', key: 'JIRA-123' },
        { type: 'known', key: 'JIRA-456' }
      ];
      expect(formatWorkKeys(workKeys)).toBe('JIRA-123, JIRA-456');
    });

    it('filters out unknown work keys', () => {
      const workKeys: WorkKey[] = [
        { type: 'known', key: 'JIRA-123' },
        { type: 'unknown' },
        { type: 'known', key: 'JIRA-456' }
      ];
      expect(formatWorkKeys(workKeys)).toBe('JIRA-123, JIRA-456');
    });

    it('returns empty string when all keys are unknown', () => {
      const workKeys: WorkKey[] = [{ type: 'unknown' }, { type: 'unknown' }];
      expect(formatWorkKeys(workKeys)).toBe('');
    });
  });

  describe('getBranchName', () => {
    it('returns name for certain branch', () => {
      const branch: BranchId = { type: 'certain', name: 'main' };
      expect(getBranchName(branch)).toBe('main');
    });

    it('returns name for inferred branch', () => {
      const branch: BranchId = { type: 'inferred', name: 'feature/test' };
      expect(getBranchName(branch)).toBe('feature/test');
    });

    it('returns empty string for nameless branch', () => {
      const branch: BranchId = { type: 'nameless', tipCommit: 'abc123' };
      expect(getBranchName(branch)).toBe('');
    });

    it('handles branch names with slashes', () => {
      const branch: BranchId = { type: 'certain', name: 'feature/JIRA-123/description' };
      expect(getBranchName(branch)).toBe('feature/JIRA-123/description');
    });
  });

  describe('formatShortHash', () => {
    it('truncates hash to 7 characters', () => {
      expect(formatShortHash('abc1234567890')).toBe('abc1234');
    });

    it('returns full hash if shorter than 7 characters', () => {
      expect(formatShortHash('abc12')).toBe('abc12');
    });

    it('handles exactly 7 character hash', () => {
      expect(formatShortHash('abc1234')).toBe('abc1234');
    });
  });

  describe('truncateMessage', () => {
    it('returns message unchanged if 100 characters or less', () => {
      const shortMessage = 'A'.repeat(100);
      expect(truncateMessage(shortMessage)).toBe(shortMessage);
    });

    it('truncates message to 100 characters with ellipsis if longer', () => {
      const longMessage = 'A'.repeat(500);
      const result = truncateMessage(longMessage);
      expect(result.length).toBe(101); // 100 chars + ellipsis
      expect(result).toBe('A'.repeat(100) + '…');
    });

    it('handles empty string', () => {
      expect(truncateMessage('')).toBe('');
    });

    it('handles exactly 101 character message', () => {
      const message = 'A'.repeat(101);
      const result = truncateMessage(message);
      expect(result).toBe('A'.repeat(100) + '…');
    });
  });
});
