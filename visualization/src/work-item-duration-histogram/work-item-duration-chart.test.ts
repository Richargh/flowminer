import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './work-item-duration-chart.ts';
import type { WorkItemDurationChart } from './work-item-duration-chart.ts';
import type { HistogramBucket } from '../data-service.ts';

describe('WorkItemDurationChart', () => {
  let element: WorkItemDurationChart;

  beforeEach(async () => {
    element = document.createElement('work-item-duration-chart') as WorkItemDurationChart;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders a container element for the chart', () => {
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

  it('has default dimensions for the chart container', () => {
    const container = element.shadowRoot?.querySelector('.chart-container') as HTMLElement;
    const computedStyle = window.getComputedStyle(container);
    expect(computedStyle.height).toBe('400px');
  });

  it('displays data as bar chart', async () => {
    const sampleBuckets: HistogramBucket[] = [
      { range: '0-5', count: 10, minValue: 0, maxValue: 5 },
      { range: '5-10', count: 25, minValue: 5, maxValue: 10 },
      { range: '10-15', count: 15, minValue: 10, maxValue: 15 },
      { range: '15-20', count: 8, minValue: 15, maxValue: 20 },
    ];

    element.data = sampleBuckets;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption).toBeTruthy();
    expect(chartOption?.series?.[0]?.type).toBe('bar');
  });

  it('updates chart when data changes', async () => {
    const initialData: HistogramBucket[] = [
      { range: '0-5', count: 10, minValue: 0, maxValue: 5 },
    ];
    const updatedData: HistogramBucket[] = [
      { range: '0-5', count: 10, minValue: 0, maxValue: 5 },
      { range: '5-10', count: 25, minValue: 5, maxValue: 10 },
      { range: '10-15', count: 15, minValue: 10, maxValue: 15 },
    ];

    element.data = initialData;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    element.data = updatedData;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption?.series?.[0]?.data?.length).toBe(3);
  });
});
