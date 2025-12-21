import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './work-item-scatter-panel';
import type { WorkItemScatterPanel } from './work-item-scatter-panel';
import type { WorkItemDuration } from '../data-service';

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
});
