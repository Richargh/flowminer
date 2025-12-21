import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './commit-timeline-chart';
import type { CommitTimelineChart } from './commit-timeline-chart';
import type { CommitTimeline } from '../data-service';

describe('CommitTimelineChart', () => {
  let element: CommitTimelineChart;

  beforeEach(async () => {
    element = document.createElement('commit-timeline-chart') as CommitTimelineChart;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders a container element for the line chart', () => {
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

  it('displays timeline data when data property is set', async () => {
    const sampleTimeline: CommitTimeline[] = [
      { date: '2024-01-01', cumulativeCount: 10, author: 'Alice' },
      { date: '2024-01-02', cumulativeCount: 15, author: 'Bob' },
      { date: '2024-01-03', cumulativeCount: 25, author: 'Alice' },
    ];

    element.data = sampleTimeline;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption).toBeTruthy();
    expect(chartOption?.series?.[0]?.type).toBe('line');
  });

  it('updates chart when data changes', async () => {
    const initialData: CommitTimeline[] = [
      { date: '2024-01-01', cumulativeCount: 10, author: 'Alice' },
    ];
    const updatedData: CommitTimeline[] = [
      { date: '2024-01-01', cumulativeCount: 10, author: 'Alice' },
      { date: '2024-01-02', cumulativeCount: 20, author: 'Bob' },
      { date: '2024-01-03', cumulativeCount: 35, author: 'Charlie' },
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
