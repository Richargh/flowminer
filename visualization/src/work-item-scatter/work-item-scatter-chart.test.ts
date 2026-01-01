import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './work-item-scatter-chart.ts';
import type { WorkItemScatterChart } from './work-item-scatter-chart.ts';
import type { WorkItemDuration } from '../data-service.ts';

describe('WorkItemScatterChart', () => {
  let element: WorkItemScatterChart;

  beforeEach(async () => {
    element = document.createElement('work-item-scatter-chart') as WorkItemScatterChart;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders a container element for the scatter chart', () => {
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

  it('displays work item data when data property is set', async () => {
    const sampleWorkItems: WorkItemDuration[] = [
      { key: 'FEAT-1', type: 'Feature', startDate: '2024-01-01', durationDays: 5 },
      { key: 'BUG-1', type: 'Bug', startDate: '2024-01-03', durationDays: 2 },
      { key: 'FEAT-2', type: 'Feature', startDate: '2024-01-05', durationDays: 8 },
    ];

    element.data = sampleWorkItems;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption).toBeTruthy();
    expect(chartOption?.series?.[0]?.type).toBe('scatter');
  });

  it('updates chart when data changes', async () => {
    const initialData: WorkItemDuration[] = [
      { key: 'FEAT-1', type: 'Feature', startDate: '2024-01-01', durationDays: 5 },
    ];
    const updatedData: WorkItemDuration[] = [
      { key: 'FEAT-1', type: 'Feature', startDate: '2024-01-01', durationDays: 5 },
      { key: 'BUG-1', type: 'Bug', startDate: '2024-01-03', durationDays: 2 },
      { key: 'FEAT-2', type: 'Feature', startDate: '2024-01-05', durationDays: 8 },
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

  it('displays average trendline using markLine', async () => {
    const sampleWorkItems: WorkItemDuration[] = [
      { key: 'FEAT-1', type: 'Feature', startDate: '2024-01-01', durationDays: 4 },
      { key: 'BUG-1', type: 'Bug', startDate: '2024-01-03', durationDays: 6 },
      { key: 'FEAT-2', type: 'Feature', startDate: '2024-01-05', durationDays: 8 },
    ];

    element.data = sampleWorkItems;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption?.series?.[0]?.markLine).toBeTruthy();
    expect(chartOption?.series?.[0]?.markLine?.data?.[0]?.type).toBe('average');
  });
});
