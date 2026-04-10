import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './ci-boxplot-chart.ts';
import type { CiBoxplotChart } from './ci-boxplot-chart.ts';
import type { BoxplotEntry } from './ci-types.ts';

describe('CiBoxplotChart', () => {
  let element: CiBoxplotChart;

  const mockData: BoxplotEntry[] = [
    { jobName: 'build', values: [60, 90, 120, 150, 240] },
    { jobName: 'test', values: [30, 45, 60, 80, 100] }
  ];

  beforeEach(async () => {
    element = document.createElement('ci-boxplot-chart') as CiBoxplotChart;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders a chart container', () => {
    const container = element.shadowRoot?.querySelector('.chart-container');
    expect(container).toBeTruthy();
  });

  it('should render chart with box-plot candles when data is set', async () => {
    element.data = mockData;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const option = element.getChartOption();
    expect(option).toBeTruthy();
    expect(option?.series?.[0]?.type).toBe('boxplot');
  });

  it('should have a tooltip that shows formatted job statistics', async () => {
    element.data = mockData;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const option = element.getChartOption();
    const formatter = option?.tooltip?.formatter;
    // Test boundary: 59s stays seconds, 60s converts to minutes
    const result = formatter?.({ dataIndex: 0, data: [59, 60, 90, 120, 240] });
    expect(result).toContain('build');
    expect(result).toContain('59.0s');  // min: 59s stays in seconds
    expect(result).toContain('1.0m');   // Q1: 60s converts to minutes
    expect(result).toContain('4.0m');   // max: 240s converts to minutes
  });

  it('should truncate y-axis job names longer than 30 characters', async () => {
    const longName = 'b'.repeat(40);
    element.data = [{ jobName: longName, values: [10, 20, 30, 40, 50] }];
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const option = element.getChartOption();
    const yAxisData = option?.yAxis?.data as string[];
    expect(yAxisData[0].length).toBeLessThanOrEqual(31);
    expect(yAxisData[0]).not.toBe(longName);
  });
});
