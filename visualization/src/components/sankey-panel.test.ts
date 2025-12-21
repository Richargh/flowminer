import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './sankey-panel';
import type { SankeyPanel } from './sankey-panel';
import type { SankeyFlow } from '../data-service';

describe('SankeyPanel', () => {
  let element: SankeyPanel;

  const mockFlow: SankeyFlow = {
    nodes: [
      { name: 'Alice' },
      { name: 'Bob' },
      { name: 'Module A' },
      { name: 'Module B' },
    ],
    links: [
      { source: 'Alice', target: 'Module A', value: 50 },
      { source: 'Alice', target: 'Module B', value: 30 },
      { source: 'Bob', target: 'Module A', value: 20 },
      { source: 'Bob', target: 'Module B', value: 40 },
    ],
  };

  beforeEach(async () => {
    element = document.createElement('sankey-panel') as SankeyPanel;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders the sankey chart', async () => {
    element.flow = mockFlow;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('sankey-chart');
    expect(chart).toBeTruthy();
  });

  it('passes flow data to chart', async () => {
    element.flow = mockFlow;
    await element.updateComplete;

    const chart = element.shadowRoot?.querySelector('sankey-chart') as HTMLElement & { data: SankeyFlow };
    expect(chart.data).toEqual(mockFlow);
  });

  it('displays author to module flow in sankey diagram', async () => {
    element.flow = mockFlow;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chart = element.shadowRoot?.querySelector('sankey-chart') as HTMLElement & { getChartOption: () => { series: { data: unknown[]; links: unknown[] }[] } | null };
    const chartOption = chart.getChartOption();
    expect(chartOption?.series?.[0]?.data?.length).toBe(4);
    expect(chartOption?.series?.[0]?.links?.length).toBe(4);
  });
});
