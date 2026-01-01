import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './commit-range-chart.ts';
import type { CommitRangeChart } from './commit-range-chart.ts';

import type {CommitActivity} from "./app/internal/commit-activity.ts";

describe('CommitRangeChart', () => {
  let element: CommitRangeChart;

  beforeEach(async () => {
    element = document.createElement('commit-range-chart') as CommitRangeChart;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders a container element for the bar chart', () => {
    const container = element.shadowRoot?.querySelector('.chart-container');
    expect(container).toBeTruthy();
    expect(container?.tagName.toLowerCase()).toBe('div');
  });

  it('initializes ECharts instance on the container', async () => {
    await new Promise(resolve => setTimeout(resolve, 100));

    const container = element.shadowRoot?.querySelector('.chart-container') as HTMLElement;
    const canvas = container?.querySelector('canvas');
    expect(canvas).toBeTruthy();
  });

  it('displays commit activity data as bar chart', async () => {
    const sampleData: CommitActivity[] = [
      { date: '2024-01-01', count: 5 },
      { date: '2024-01-02', count: 10 },
      { date: '2024-01-03', count: 3 },
    ];

    element.data = sampleData;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption).toBeTruthy();
    expect(chartOption?.series?.[0]?.type).toBe('bar');
  });

  it('includes dataZoom for range selection', async () => {
    const sampleData: CommitActivity[] = [
      { date: '2024-01-01', count: 5 },
      { date: '2024-01-02', count: 10 },
    ];

    element.data = sampleData;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption?.dataZoom).toBeTruthy();
    expect(chartOption?.dataZoom?.length).toBeGreaterThan(0);
  });

  it('uses dates on x-axis and counts on y-axis', async () => {
    const sampleData: CommitActivity[] = [
      { date: '2024-01-01', count: 5 },
      { date: '2024-01-02', count: 10 },
    ];

    element.data = sampleData;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption?.xAxis?.data).toEqual(['2024-01-01', '2024-01-02']);
    expect(chartOption?.series?.[0]?.data).toEqual([5, 10]);
  });

  it('emits time-range-changed event when dataZoom changes', async () => {
    const sampleData: CommitActivity[] = [
      { date: '2024-01-01', count: 5 },
      { date: '2024-01-02', count: 10 },
      { date: '2024-01-03', count: 3 },
      { date: '2024-01-04', count: 8 },
    ];

    element.data = sampleData;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const eventPromise = new Promise<CustomEvent>((resolve) => {
      element.addEventListener('time-range-changed', (e) => resolve(e as CustomEvent), { once: true });
    });

    // Simulate dataZoom by calling the handler directly with mock event data
    element.simulateDataZoomEvent({ start: 25, end: 75 });

    const event = await eventPromise;
    expect(event.detail).toHaveProperty('startDate');
    expect(event.detail).toHaveProperty('endDate');
    expect(event.detail.startDate).toBe('2024-01-02');
    expect(event.detail.endDate).toBe('2024-01-03');
  });

  it('has brush selection enabled for direct range selection', async () => {
    const sampleData: CommitActivity[] = [
      { date: '2024-01-01', count: 5 },
      { date: '2024-01-02', count: 10 },
    ];

    element.data = sampleData;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption?.brush).toBeTruthy();
    expect(chartOption?.brush?.xAxisIndex).toBe(0);
    expect(chartOption?.brush?.brushType).toBe('lineX');
  });

  it('emits time-range-changed event when brush selection changes', async () => {
    const sampleData: CommitActivity[] = [
      { date: '2024-01-01', count: 5 },
      { date: '2024-01-02', count: 10 },
      { date: '2024-01-03', count: 3 },
      { date: '2024-01-04', count: 8 },
    ];

    element.data = sampleData;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const eventPromise = new Promise<CustomEvent>((resolve) => {
      element.addEventListener('time-range-changed', (e) => resolve(e as CustomEvent), { once: true });
    });

    // Simulate brush selection by calling the handler directly
    element.simulateBrushEvent({ startIndex: 1, endIndex: 2 });

    const event = await eventPromise;
    expect(event.detail.startDate).toBe('2024-01-02');
    expect(event.detail.endDate).toBe('2024-01-03');
  });
});
