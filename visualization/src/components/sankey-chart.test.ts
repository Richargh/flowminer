import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './sankey-chart';
import type { SankeyChart } from './sankey-chart';
import type { SankeyFlow } from '../data-service';

describe('SankeyChart', () => {
  let element: SankeyChart;

  beforeEach(async () => {
    element = document.createElement('sankey-chart') as SankeyChart;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders a container element for the sankey chart', () => {
    const container = element.shadowRoot?.querySelector('.chart-container');
    expect(container).toBeTruthy();
    expect(container?.tagName.toLowerCase()).toBe('div');
  });

  it('initializes ECharts instance on the container', async () => {
    // Sankey charts need data to render properly
    element.data = {
      nodes: [{ name: 'A' }, { name: 'B' }],
      links: [{ source: 'A', target: 'B', value: 10 }],
    };
    await element.updateComplete;
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

  it('displays sankey data when data property is set', async () => {
    const sampleFlow: SankeyFlow = {
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

    element.data = sampleFlow;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption).toBeTruthy();
    expect(chartOption?.series?.[0]?.type).toBe('sankey');
  });

  it('updates chart when data changes', async () => {
    const initialFlow: SankeyFlow = {
      nodes: [{ name: 'A' }, { name: 'B' }],
      links: [{ source: 'A', target: 'B', value: 10 }],
    };
    const updatedFlow: SankeyFlow = {
      nodes: [{ name: 'A' }, { name: 'B' }, { name: 'C' }],
      links: [
        { source: 'A', target: 'B', value: 10 },
        { source: 'B', target: 'C', value: 5 },
      ],
    };

    element.data = initialFlow;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    element.data = updatedFlow;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption?.series?.[0]?.data?.length).toBe(3);
    expect(chartOption?.series?.[0]?.links?.length).toBe(2);
  });
});
