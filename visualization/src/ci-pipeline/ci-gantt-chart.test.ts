import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './ci-gantt-chart.ts';
import type { CiGanttChart } from './ci-gantt-chart.ts';
import type { GanttEntry } from './ci-gantt-chart.ts';

describe('CiGanttChart', () => {
  let element: CiGanttChart;

  const pipeline1Entry: GanttEntry = { jobName: 'lint', jobStatus: 'success', pipelineId: 1, offsetSeconds: 60, durationSeconds: 90 };
  const pipeline2Entry: GanttEntry = { jobName: 'build', jobStatus: 'failed', pipelineId: 2, offsetSeconds: 60, durationSeconds: 120 };

  beforeEach(async () => {
    element = document.createElement('ci-gantt-chart') as CiGanttChart;
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

  it('should have a tooltip configured', async () => {
    element.data = [pipeline1Entry];
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const option = element.getChartOption();
    const formatter = option?.tooltip?.formatter;
    const result = formatter?.({
      data: { jobName: 'lint', jobStatus: 'success', durationSeconds: 90 }
    });
    expect(result).toContain('lint');
    expect(result).toContain('success');
    expect(result).toContain('1.5m');
  });

  it('should truncate y-axis job names longer than 30 characters', async () => {
    const longName = 'b'.repeat(40);
    element.data = [{ jobName: longName, jobStatus: 'success', pipelineId: 1, offsetSeconds: 0, durationSeconds: 60 }];
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const option = element.getChartOption();
    const yAxisData = option?.yAxis?.data as string[];
    expect(yAxisData[0].length).toBeLessThanOrEqual(31);
    expect(yAxisData[0]).not.toBe(longName);
  });

  it('should order y-axis by job start time (earliest first)', async () => {
    element.data = [
      { jobName: 'deploy', jobStatus: 'success', pipelineId: 1, offsetSeconds: 300, durationSeconds: 60 },
      { jobName: 'lint',   jobStatus: 'success', pipelineId: 1, offsetSeconds: 0,   durationSeconds: 90 },
      { jobName: 'build',  jobStatus: 'success', pipelineId: 1, offsetSeconds: 120, durationSeconds: 120 },
    ];
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const option = element.getChartOption();
    const yAxisData = option?.yAxis?.data as string[];
    expect(yAxisData[0]).toBe('lint');
    expect(yAxisData[1]).toBe('build');
    expect(yAxisData[2]).toBe('deploy');
  });

  it('should assign distinct colour indices per pipeline', async () => {
    element.data = [pipeline1Entry, pipeline2Entry];
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const option = element.getChartOption();
    const seriesData = (option?.series?.[0] as { data: Array<{ itemStyle: { color: string } }> })?.data;
    expect(seriesData).toHaveLength(2);

    const colour1 = seriesData[0].itemStyle.color;
    const colour2 = seriesData[1].itemStyle.color;
    expect(colour1).not.toBe(colour2);
  });
});
