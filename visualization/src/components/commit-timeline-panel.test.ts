import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './commit-timeline-panel';
import type { CommitTimelinePanel } from './commit-timeline-panel';
import type { CommitTimeline } from '../data-service';

describe('CommitTimelinePanel', () => {
  let element: CommitTimelinePanel;

  const mockTimeline: CommitTimeline[] = [
    { date: '2024-01-01', cumulativeCount: 10, author: 'Alice' },
    { date: '2024-01-02', cumulativeCount: 25, author: 'Bob' },
    { date: '2024-01-03', cumulativeCount: 40, author: 'Alice' },
  ];

  beforeEach(async () => {
    element = document.createElement('commit-timeline-panel') as CommitTimelinePanel;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders the commit timeline chart', async () => {
    element.timeline = mockTimeline;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('commit-timeline-chart');
    expect(chart).toBeTruthy();
  });

  it('passes timeline data to chart', async () => {
    element.timeline = mockTimeline;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('commit-timeline-chart') as HTMLElement & { data: CommitTimeline[] };
    expect(chart.data).toEqual(mockTimeline);
  });

  it('displays cumulative commits over time', async () => {
    element.timeline = mockTimeline;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chart = element.shadowRoot?.querySelector('commit-timeline-chart') as HTMLElement & { getChartOption: () => { series: { data: number[] }[] } | null };
    const chartOption = chart.getChartOption();
    expect(chartOption?.series?.[0]?.data).toEqual([10, 25, 40]);
  });
});
