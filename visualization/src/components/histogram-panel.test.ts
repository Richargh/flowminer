import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import './histogram-panel';
import type { HistogramPanel } from './histogram-panel';
import type { HistogramBucket } from '../data-service';

describe('HistogramPanel', () => {
  let element: HistogramPanel;

  const mockBuckets: HistogramBucket[] = [
    { range: '0-5', count: 10, minValue: 0, maxValue: 5 },
    { range: '5-10', count: 25, minValue: 5, maxValue: 10 },
    { range: '10-15', count: 15, minValue: 10, maxValue: 15 },
    { range: '15-20', count: 8, minValue: 15, maxValue: 20 },
  ];

  beforeEach(async () => {
    element = document.createElement('histogram-panel') as HistogramPanel;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders the histogram chart', async () => {
    element.buckets = mockBuckets;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('histogram-chart');
    expect(chart).toBeTruthy();
  });

  it('passes bucket data to chart', async () => {
    element.buckets = mockBuckets;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('histogram-chart') as HTMLElement & { data: HistogramBucket[] };
    expect(chart.data).toEqual(mockBuckets);
  });

  it('displays histogram distribution', async () => {
    element.buckets = mockBuckets;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chart = element.shadowRoot?.querySelector('histogram-chart') as HTMLElement & { getChartOption: () => { series: { data: number[] }[]; dataZoom: unknown[] } | null };
    const chartOption = chart.getChartOption();
    expect(chartOption?.series?.[0]?.data?.length).toBe(4);
    expect(chartOption?.dataZoom?.length).toBeGreaterThan(0);
  });

  it('dispatches range-change event when range selection changes', async () => {
    element.buckets = mockBuckets;
    await element.updateComplete;

    const rangeChangeHandler = vi.fn();
    element.addEventListener('range-change', rangeChangeHandler);

    // Simulate a range change by calling the method directly
    element.dispatchRangeChange(0, 50);

    expect(rangeChangeHandler).toHaveBeenCalledTimes(1);
    const event = rangeChangeHandler.mock.calls[0][0] as CustomEvent;
    expect(event.detail.start).toBe(0);
    expect(event.detail.end).toBe(50);
  });
});
