import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './commits-table-panel.ts';
import type { CommitsTablePanel } from './commits-table-panel.ts';
import type { GitMiningResult } from '../commit-mining/app/api-types/git-mining-result.ts';
import { Commits } from '../commit-mining/app/api-types/git-mining-result.ts';
import type { Commit } from '../commit/app/api-types/commit.ts';

describe('CommitsTablePanel', () => {
  let element: CommitsTablePanel;

  const mockCommit: Commit = {
    hash: 'abc1234567890',
    author: { name: 'Alice', email: 'alice@example.com' },
    date: new Date('2024-01-15').getTime(),
    message: 'Add feature X',
    parents: [],
    fileChanges: [],
    coAuthors: [],
    commitType: 'FEATURE',
    workKeys: [{ type: 'known', key: 'JIRA-123' }],
    branchId: { type: 'certain', name: 'main' },
    isOnCurrentBranch: true,
    isMerge: false
  };

  beforeEach(async () => {
    element = document.createElement('commits-table-panel') as CommitsTablePanel;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders without error when created', () => {
    expect(element).toBeTruthy();
  });

  it('updates commits on data-loaded event', async () => {
    const mockResult = {
      commits: new Commits([mockCommit])
    } as GitMiningResult;

    document.dispatchEvent(new CustomEvent<GitMiningResult>('data-loaded', {
      detail: mockResult,
      bubbles: true
    }));

    await element.updateComplete;

    expect(element.commits).toHaveLength(1);
    expect(element.commits[0].hash).toBe('abc1234567890');
  });

  it('configures all commit columns', async () => {
    const dataTable = element.querySelector('data-table');
    expect(dataTable).toBeTruthy();

    // Access columns through the data-table element
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const columns = (dataTable as any).columns;
    const headers = columns.map((c: { header: string }) => c.header);

    expect(headers).toContain('Hash');
    expect(headers).toContain('Author');
    expect(headers).toContain('Date');
    expect(headers).toContain('Message');
    expect(headers).toContain('Type');
    expect(headers).toContain('Work Keys');
    expect(headers).toContain('Branch');
    expect(columns).toHaveLength(7);
  });

  it('shows empty state message when no commits', async () => {
    // Commits array is empty by default
    expect(element.commits).toHaveLength(0);

    const dataTable = element.querySelector('data-table');
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const emptyMessage = (dataTable as any).emptyMessage;

    expect(emptyMessage).toBe('No commits in selected range');
  });

  it('updates commits on data-constrained event', async () => {
    const filteredCommit: Commit = {
      hash: 'filtered123456',
      author: { name: 'Bob', email: 'bob@example.com' },
      date: new Date('2024-02-01').getTime(),
      message: 'Filtered commit',
      parents: [],
      fileChanges: [],
      coAuthors: [],
      commitType: 'FIX',
      workKeys: [{ type: 'known', key: 'JIRA-456' }],
      branchId: { type: 'certain', name: 'develop' },
      isOnCurrentBranch: true,
      isMerge: false
    };

    const mockResult = {
      commits: new Commits([filteredCommit])
    } as GitMiningResult;

    document.dispatchEvent(new CustomEvent<GitMiningResult>('data-constrained', {
      detail: mockResult,
      bubbles: true
    }));

    await element.updateComplete;

    expect(element.commits).toHaveLength(1);
    expect(element.commits[0].hash).toBe('filtered123456');
  });

  it('truncates long commit messages', async () => {
    const longMessage = 'A'.repeat(500);
    const commitWithLongMessage: Commit = {
      ...mockCommit,
      message: longMessage
    };

    const mockResult = {
      commits: new Commits([commitWithLongMessage])
    } as GitMiningResult;

    document.dispatchEvent(new CustomEvent<GitMiningResult>('data-loaded', {
      detail: mockResult,
      bubbles: true
    }));

    await element.updateComplete;

    const dataTable = element.querySelector('data-table');
    await (dataTable as { updateComplete: Promise<boolean> }).updateComplete;

    const messageCell = dataTable?.querySelector('td:nth-child(4)');
    expect(messageCell?.textContent?.length).toBeLessThan(150);
  });

  it('renders special characters in author name correctly', async () => {
    const commitWithSpecialAuthor: Commit = {
      ...mockCommit,
      author: { name: 'François Müller <测试>', email: 'test@example.com' }
    };

    const mockResult = {
      commits: new Commits([commitWithSpecialAuthor])
    } as GitMiningResult;

    document.dispatchEvent(new CustomEvent<GitMiningResult>('data-loaded', {
      detail: mockResult,
      bubbles: true
    }));

    await element.updateComplete;

    const dataTable = element.querySelector('data-table');
    await (dataTable as { updateComplete: Promise<boolean> }).updateComplete;

    const authorCell = dataTable?.querySelector('td:nth-child(2)');
    expect(authorCell?.textContent).toContain('François Müller <测试>');
  });

  it('handles unknown work keys', async () => {
    const commitWithUnknownWorkKey: Commit = {
      ...mockCommit,
      workKeys: [{ type: 'unknown' }]
    };

    const mockResult = {
      commits: new Commits([commitWithUnknownWorkKey])
    } as GitMiningResult;

    document.dispatchEvent(new CustomEvent<GitMiningResult>('data-loaded', {
      detail: mockResult,
      bubbles: true
    }));

    await element.updateComplete;

    const dataTable = element.querySelector('data-table');
    await (dataTable as { updateComplete: Promise<boolean> }).updateComplete;

    const workKeysCell = dataTable?.querySelector('td:nth-child(6)');
    // Unknown work keys should result in empty string (filtered out)
    expect(workKeysCell?.textContent?.trim()).toBe('');
  });

  it('displays branch names with slashes correctly', async () => {
    const commitWithSlashedBranch: Commit = {
      ...mockCommit,
      branchId: { type: 'certain', name: 'feature/JIRA-123/description' }
    };

    const mockResult = {
      commits: new Commits([commitWithSlashedBranch])
    } as GitMiningResult;

    document.dispatchEvent(new CustomEvent<GitMiningResult>('data-loaded', {
      detail: mockResult,
      bubbles: true
    }));

    await element.updateComplete;

    const dataTable = element.querySelector('data-table');
    await (dataTable as { updateComplete: Promise<boolean> }).updateComplete;

    const branchCell = dataTable?.querySelector('td:nth-child(7)');
    expect(branchCell?.textContent).toContain('feature/JIRA-123/description');
  });
});
