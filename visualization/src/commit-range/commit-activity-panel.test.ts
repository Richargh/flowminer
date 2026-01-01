import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import './commit-range-panel.ts';
import type { CommitRangePanel } from './commit-range-panel.ts';

import type {CommitActivity} from "./app/internal/commit-activity.ts";

describe('CommitRangePanel', () => {
  let element: CommitRangePanel;

  const mockData: CommitActivity[] = [
    { date: '2024-01-01', count: 5 },
    { date: '2024-01-02', count: 10 },
    { date: '2024-01-03', count: 3 },
    { date: '2024-01-04', count: 8 },
  ];

  beforeEach(async () => {
    element = document.createElement('commit-range-panel') as CommitRangePanel;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders the commit activity chart', async () => {
    element.data = mockData;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('commit-range-chart');
    expect(chart).toBeTruthy();
  });

  it('passes data to chart', async () => {
    element.data = mockData;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('commit-range-chart') as HTMLElement & { data: CommitActivity[] };
    expect(chart.data).toEqual(mockData);
  });

  it('exposes time-range-changed event from chart', async () => {
    element.data = mockData;
    await element.updateComplete;

    const timeRangeHandler = vi.fn();
    element.addEventListener('time-range-changed', timeRangeHandler);

    const chart = element.shadowRoot?.querySelector('commit-range-chart') as HTMLElement & { simulateDataZoomEvent: (params: { start: number; end: number }) => void };
    await new Promise(resolve => setTimeout(resolve, 100));

    chart.simulateDataZoomEvent({ start: 25, end: 75 });

    expect(timeRangeHandler).toHaveBeenCalledTimes(1);
    const event = timeRangeHandler.mock.calls[0][0] as CustomEvent;
    expect(event.detail.startDate).toBe('2024-01-02');
    expect(event.detail.endDate).toBe('2024-01-03');
  });
});
