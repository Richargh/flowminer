import { describe, it, expect, vi } from 'vitest';
import { GitMiningCoordinator } from './git-mining-coordinator.ts';
import type { GitMiningResult } from './app/api-types/git-mining-result.ts';
import { Commits } from './app/api-types/git-mining-result.ts';
import { WorkItems, type WorkItem } from './app/api-types/work-items.ts';
import { Branches } from './app/api-types/branch.ts';
import { AuthorStatistics } from './app/api-types/author.ts';

function createWorkItem(firstCommitDate: Date): WorkItem {
  return {
    workKey: { type: 'known', key: `WORK-${firstCommitDate.getTime()}` },
    linesAdded: 100,
    linesRemoved: 50,
    firstCommitDate,
    lastCommitDate: firstCommitDate,
    filesChanged: ['file.ts'],
    contributions: [],
    absoluteChurnByType: new Map(),
    commits: 1,
    collaborators: 1,
    reworkFiles: []
  };
}

describe('GitMiningCoordinator', () => {
  it('should store the full result and return it via getFullResult', () => {
    const coordinator = new GitMiningCoordinator();
    const mockResult: GitMiningResult = {
      commits: new Commits([]),
      branches: new Branches([]),
      workItems: new WorkItems([]),
      authorStatistics: new AuthorStatistics([])
    };

    coordinator.storeResult(mockResult);

    expect(coordinator.getFullResult()).toBe(mockResult);
  });

  it('should dispatch data-constrained event when setRange is called', () => {
    const coordinator = new GitMiningCoordinator();
    const mockResult: GitMiningResult = {
      commits: new Commits([]),
      branches: new Branches([]),
      workItems: new WorkItems([]),
      authorStatistics: new AuthorStatistics([])
    };

    coordinator.storeResult(mockResult);

    const dataConstrainedHandler = vi.fn();
    document.addEventListener('data-constrained', dataConstrainedHandler);

    coordinator.setRange({ startDate: '2024-01-02', endDate: '2024-01-03' });

    expect(dataConstrainedHandler).toHaveBeenCalledTimes(1);

    document.removeEventListener('data-constrained', dataConstrainedHandler);
  });

  it('should dispatch data-constrained with filtered work items based on range', () => {
    const coordinator = new GitMiningCoordinator();
    const workItems = [
      createWorkItem(new Date('2024-01-01')),
      createWorkItem(new Date('2024-01-05')),
      createWorkItem(new Date('2024-01-10'))
    ];

    const mockResult: GitMiningResult = {
      commits: new Commits([]),
      branches: new Branches([]),
      workItems: new WorkItems(workItems),
      authorStatistics: new AuthorStatistics([])
    };

    coordinator.storeResult(mockResult);

    const dataConstrainedHandler = vi.fn();
    document.addEventListener('data-constrained', dataConstrainedHandler);

    coordinator.setRange({ startDate: '2024-01-03', endDate: '2024-01-08' });

    expect(dataConstrainedHandler).toHaveBeenCalledTimes(1);
    const event = dataConstrainedHandler.mock.calls[0][0] as CustomEvent<GitMiningResult>;
    expect(event.detail.workItems.size()).toBe(1);
    expect(event.detail.workItems.all()[0].firstCommitDate).toEqual(new Date('2024-01-05'));

    document.removeEventListener('data-constrained', dataConstrainedHandler);
  });

  it('should dispatch full data when range matches full data range', () => {
    const coordinator = new GitMiningCoordinator();
    const workItems = [
      createWorkItem(new Date('2024-01-01')),
      createWorkItem(new Date('2024-01-05')),
      createWorkItem(new Date('2024-01-10'))
    ];

    const mockResult: GitMiningResult = {
      commits: new Commits([]),
      branches: new Branches([]),
      workItems: new WorkItems(workItems),
      authorStatistics: new AuthorStatistics([])
    };

    coordinator.storeResult(mockResult);

    const dataConstrainedHandler = vi.fn();
    document.addEventListener('data-constrained', dataConstrainedHandler);

    coordinator.setRange({ startDate: '2024-01-01', endDate: '2024-01-10' });

    expect(dataConstrainedHandler).toHaveBeenCalledTimes(1);
    const event = dataConstrainedHandler.mock.calls[0][0] as CustomEvent<GitMiningResult>;
    expect(event.detail.workItems.size()).toBe(3);

    document.removeEventListener('data-constrained', dataConstrainedHandler);
  });

  it('should dispatch empty workItems when range excludes all items', () => {
    const coordinator = new GitMiningCoordinator();
    const workItems = [
      createWorkItem(new Date('2024-01-01')),
      createWorkItem(new Date('2024-01-02')),
      createWorkItem(new Date('2024-01-03'))
    ];

    const mockResult: GitMiningResult = {
      commits: new Commits([]),
      branches: new Branches([]),
      workItems: new WorkItems(workItems),
      authorStatistics: new AuthorStatistics([])
    };

    coordinator.storeResult(mockResult);

    const dataConstrainedHandler = vi.fn();
    document.addEventListener('data-constrained', dataConstrainedHandler);

    coordinator.setRange({ startDate: '2024-06-01', endDate: '2024-06-30' });

    expect(dataConstrainedHandler).toHaveBeenCalledTimes(1);
    const event = dataConstrainedHandler.mock.calls[0][0] as CustomEvent<GitMiningResult>;
    expect(event.detail.workItems.size()).toBe(0);

    document.removeEventListener('data-constrained', dataConstrainedHandler);
  });

  it('should not dispatch when no result is stored', () => {
    const coordinator = new GitMiningCoordinator();

    const dataConstrainedHandler = vi.fn();
    document.addEventListener('data-constrained', dataConstrainedHandler);

    coordinator.setRange({ startDate: '2024-01-01', endDate: '2024-01-10' });

    expect(dataConstrainedHandler).not.toHaveBeenCalled();

    document.removeEventListener('data-constrained', dataConstrainedHandler);
  });
});
