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
    date: new Date('2024-01-15'),
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
    expect(element.shadowRoot).toBeTruthy();
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

  it('updates commits on data-constrained event', async () => {
    const filteredCommit: Commit = {
      hash: 'filtered123456',
      author: { name: 'Bob', email: 'bob@example.com' },
      date: new Date('2024-02-01'),
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
});
