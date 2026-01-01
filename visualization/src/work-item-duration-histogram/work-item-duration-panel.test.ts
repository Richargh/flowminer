import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './work-item-duration-panel.ts';
import type { WorkItemDurationPanel } from './work-item-duration-panel.ts';
import type { HistogramBucket } from '../data-service.ts';
import type { GitMiningResult } from '../commit-mining/app/api-types/git-mining-result.ts';
import { WorkItems, type WorkItem } from '../commit-mining/app/api-types/work-items.ts';

describe('WorkItemDurationPanel', () => {
  let element: WorkItemDurationPanel;

  const mockBuckets: HistogramBucket[] = [
    { range: '0-5', count: 10, minValue: 0, maxValue: 5 },
    { range: '5-10', count: 25, minValue: 5, maxValue: 10 },
    { range: '10-15', count: 15, minValue: 10, maxValue: 15 },
    { range: '15-20', count: 8, minValue: 15, maxValue: 20 },
  ];

  beforeEach(async () => {
    element = document.createElement('work-item-duration-panel') as WorkItemDurationPanel;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders the chart', async () => {
    element.buckets = mockBuckets;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('work-item-duration-chart');
    expect(chart).toBeTruthy();
  });

  it('passes bucket data to chart', async () => {
    element.buckets = mockBuckets;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('work-item-duration-chart') as HTMLElement & { data: HistogramBucket[] };
    expect(chart.data).toEqual(mockBuckets);
  });

  it('displays distribution', async () => {
    element.buckets = mockBuckets;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chart = element.shadowRoot?.querySelector('work-item-duration-chart') as HTMLElement & { getChartOption: () => { series: { data: number[] }[] } | null };
    const chartOption = chart.getChartOption();
    expect(chartOption?.series?.[0]?.data?.length).toBe(4);
  });

  it('updates buckets when data-loaded event is received', async () => {
    const mockWorkItems: WorkItem[] = [
      {
        workKey: { type: 'known', key: 'FEAT-1' },
        linesAdded: 100,
        linesRemoved: 50,
        firstCommitDate: new Date('2024-01-01'),
        lastCommitDate: new Date('2024-01-02'), // 1 day - bucket 0-2
        filesChanged: ['file.ts'],
        contributions: [],
        absoluteChurnByType: new Map([['FEATURE', 150]]),
        commits: 2,
        collaborators: 1,
        reworkFiles: []
      },
      {
        workKey: { type: 'known', key: 'FEAT-2' },
        linesAdded: 200,
        linesRemoved: 100,
        firstCommitDate: new Date('2024-01-01'),
        lastCommitDate: new Date('2024-01-04'), // 3 days - bucket 2-4
        filesChanged: ['file2.ts'],
        contributions: [],
        absoluteChurnByType: new Map([['FEATURE', 300]]),
        commits: 5,
        collaborators: 2,
        reworkFiles: []
      }
    ];

    const mockResult = {
      workItems: new WorkItems(mockWorkItems)
    } as GitMiningResult;

    document.dispatchEvent(new CustomEvent<GitMiningResult>('data-loaded', {
      detail: mockResult,
      bubbles: true
    }));

    await element.updateComplete;

    expect(element.buckets).toHaveLength(6); // 6 predefined bucket ranges
    expect(element.buckets[0].count).toBe(1); // 0-2 days bucket has 1 item
    expect(element.buckets[1].count).toBe(1); // 2-4 days bucket has 1 item
  });
});
