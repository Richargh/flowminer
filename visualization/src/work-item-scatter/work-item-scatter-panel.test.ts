import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './work-item-scatter-panel.ts';
import type { WorkItemScatterPanel } from './work-item-scatter-panel.ts';
import type { WorkItemDuration } from '../data-service.ts';
import type { GitMiningResult } from '../commit-mining/app/api-types/git-mining-result.ts';
import { WorkItems, type WorkItem } from '../commit-mining/app/api-types/work-items.ts';

describe('WorkItemScatterPanel', () => {
  let element: WorkItemScatterPanel;

  const mockWorkItems: WorkItemDuration[] = [
    { key: 'FEAT-1', type: 'Feature', startDate: '2024-01-01', durationDays: 5 },
    { key: 'BUG-1', type: 'Bug', startDate: '2024-01-03', durationDays: 2 },
    { key: 'FEAT-2', type: 'Feature', startDate: '2024-01-05', durationDays: 8 },
  ];

  beforeEach(async () => {
    element = document.createElement('work-item-scatter-panel') as WorkItemScatterPanel;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders the work item scatter chart', async () => {
    element.workItems = mockWorkItems;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('work-item-scatter-chart');
    expect(chart).toBeTruthy();
  });

  it('passes work item data to chart', async () => {
    element.workItems = mockWorkItems;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('work-item-scatter-chart') as HTMLElement & { data: WorkItemDuration[] };
    expect(chart.data).toEqual(mockWorkItems);
  });

  it('displays work item durations over time with trendline', async () => {
    element.workItems = mockWorkItems;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chart = element.shadowRoot?.querySelector('work-item-scatter-chart') as HTMLElement & { getChartOption: () => { series: { data: unknown[]; markLine: unknown }[] } | null };
    const chartOption = chart.getChartOption();
    expect(chartOption?.series?.[0]?.data?.length).toBe(3);
    expect(chartOption?.series?.[0]?.markLine).toBeTruthy();
  });

  it('updates workItems when data-loaded event is received', async () => {
    const mockWorkItem: WorkItem = {
      workKey: { type: 'known', key: 'TEST-123' },
      linesAdded: 100,
      linesRemoved: 50,
      firstCommitDate: new Date('2024-01-01'),
      lastCommitDate: new Date('2024-01-06'),
      filesChanged: ['file.ts'],
      contributions: [],
      absoluteChurnByType: new Map([['FEATURE', 150]]),
      commits: 3,
      collaborators: 1,
      reworkFiles: []
    };

    const mockResult = {
      workItems: new WorkItems([mockWorkItem])
    } as GitMiningResult;

    document.dispatchEvent(new CustomEvent<GitMiningResult>('data-loaded', {
      detail: mockResult,
      bubbles: true
    }));

    await element.updateComplete;

    expect(element.workItems).toHaveLength(1);
    expect(element.workItems[0].key).toBe('TEST-123');
    expect(element.workItems[0].durationDays).toBe(5);
  });

  it('updates workItems when data-constrained event is received', async () => {
    const mockWorkItem: WorkItem = {
      workKey: { type: 'known', key: 'FILTERED-1' },
      linesAdded: 50,
      linesRemoved: 25,
      firstCommitDate: new Date('2024-02-01'),
      lastCommitDate: new Date('2024-02-03'),
      filesChanged: ['filtered.ts'],
      contributions: [],
      absoluteChurnByType: new Map([['FIX', 75]]),
      commits: 2,
      collaborators: 1,
      reworkFiles: []
    };

    const mockResult = {
      workItems: new WorkItems([mockWorkItem])
    } as GitMiningResult;

    document.dispatchEvent(new CustomEvent<GitMiningResult>('data-constrained', {
      detail: mockResult,
      bubbles: true
    }));

    await element.updateComplete;

    expect(element.workItems).toHaveLength(1);
    expect(element.workItems[0].key).toBe('FILTERED-1');
    expect(element.workItems[0].durationDays).toBe(2);
  });

  it('shows empty state message when workItems is empty', async () => {
    const mockResult = {
      workItems: new WorkItems([])
    } as GitMiningResult;

    document.dispatchEvent(new CustomEvent<GitMiningResult>('data-constrained', {
      detail: mockResult,
      bubbles: true
    }));

    await element.updateComplete;

    const emptyState = element.shadowRoot?.querySelector('.empty-state');
    expect(emptyState).toBeTruthy();
    expect(emptyState?.textContent).toContain('No work items in selected range');
  });
});
