import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './work-item-duration-panel.ts';
import type { WorkItemDurationPanel } from './work-item-duration-panel.ts';
import type { HistogramBucket } from '../data-service.ts';

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
});
