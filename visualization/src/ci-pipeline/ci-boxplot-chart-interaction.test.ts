import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { userEvent } from 'vitest/browser';
import './ci-boxplot-chart.ts';
import type { CiBoxplotChart } from './ci-boxplot-chart.ts';
import type { BoxplotEntry } from './ci-types.ts';

describe('CiBoxplotChart - right-click interaction', () => {
  let element: CiBoxplotChart;

  const mockData: BoxplotEntry[] = [
    { jobName: 'build', values: [60, 90, 120, 150, 240] },
    { jobName: 'test',  values: [30, 45,  60,  80, 100] }
  ];

  beforeEach(async () => {
    element = document.createElement('ci-boxplot-chart') as CiBoxplotChart;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('should exclude a job when right-clicking its bar in the chart', async () => {
    element.data = mockData;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 200));

    const container = element.shadowRoot!.querySelector('.chart-container') as HTMLElement;
    const canvas = container.querySelector('canvas')!;
    const { width, height } = canvas.getBoundingClientRect();

    // Right-click anywhere in the 'build' row.
    // ECharts renders categories bottom-to-top; 'build' (index 0) is in the lower
    // half of the plot area. With default grid (top: 60px, bottom: 60px) in a
    // 300px chart, 'build' centre ≈ 195px from top (65%).
    // x=50% is well inside the plot area so convertFromPixel can resolve the row.
    await userEvent.click(canvas, {
      button: 'right',
      position: { x: Math.round(width * 0.5), y: Math.round(height * 0.65) },
    });

    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 50));

    const option = element.getChartOption();
    const yAxisData = option?.yAxis?.data as string[];
    expect(yAxisData).not.toContain('build');
    expect(yAxisData).toContain('test');
  });
});
