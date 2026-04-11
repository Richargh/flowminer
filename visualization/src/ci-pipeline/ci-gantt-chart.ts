import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { echarts } from '../echarts-setup.ts';
import type { EChartsType } from 'echarts/core';
import { truncate, MAX_LABEL_LENGTH, formatDuration } from './ci-chart-utils.ts';

export interface GanttEntry {
  readonly jobName: string;
  readonly jobStatus: string;
  readonly pipelineId: number;
  readonly offsetSeconds: number;
  readonly durationSeconds: number;
}

const COLOUR_PALETTE = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc',
];

export interface GanttChartOption {
  tooltip: { formatter: (params: unknown) => string };
  xAxis: { type: 'value'; name: string };
  yAxis: { type: 'category'; data: string[] };
  series: unknown[];
}

@customElement('ci-gantt-chart')
export class CiGanttChart extends LitElement {
  private chart: EChartsType | null = null;
  private currentOption: GanttChartOption | null = null;
  private resizeObserver: ResizeObserver | null = null;

  @property({ type: Array })
  data: GanttEntry[] = [];

  static styles = css`
    :host { display: block; }
    .chart-container { width: 100%; height: 400px; }
  `;

  render() {
    return html`<div class="chart-container"></div>`;
  }

  protected firstUpdated(): void {
    const container = this.shadowRoot?.querySelector('.chart-container') as HTMLElement;
    if (!container) return;

    this.resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        if (width > 0 && height > 0) {
          if (!this.chart) {
            this.chart = echarts.init(container);
            this.updateChartWithData();
          } else {
            this.chart.resize();
          }
        }
      }
    });
    this.resizeObserver.observe(container);
  }

  protected updated(changedProperties: Map<string, unknown>): void {
    if (changedProperties.has('data') && this.chart) {
      this.updateChartWithData();
    }
  }

  private buildColourMap(entries: GanttEntry[]): Map<number, string> {
    const pipelineIds = [...new Set(entries.map(e => e.pipelineId))];
    const colourMap = new Map<number, string>();
    pipelineIds.forEach((id, index) => {
      colourMap.set(id, COLOUR_PALETTE[index % COLOUR_PALETTE.length]);
    });
    return colourMap;
  }

  private updateChartWithData(): void {
    if (!this.chart) return;

    const entries = this.data;
    const colourMap = this.buildColourMap(entries);

    const earliestOffsetByJob = new Map<string, number>();
    for (const e of entries) {
      const current = earliestOffsetByJob.get(e.jobName) ?? Infinity;
      if (e.offsetSeconds < current) earliestOffsetByJob.set(e.jobName, e.offsetSeconds);
    }
    const uniqueJobNames = [...new Set(entries.map(e => e.jobName))]
      .sort((a, b) => (earliestOffsetByJob.get(a) ?? 0) - (earliestOffsetByJob.get(b) ?? 0));
    const yAxisLabels = uniqueJobNames.map(name => truncate(name, MAX_LABEL_LENGTH));

    const seriesData = entries.map(e => {
      const yIndex = uniqueJobNames.indexOf(e.jobName);
      return {
        value: [yIndex, e.offsetSeconds, e.offsetSeconds + e.durationSeconds],
        itemStyle: { color: colourMap.get(e.pipelineId) ?? COLOUR_PALETTE[0] },
        jobName: e.jobName,
        jobStatus: e.jobStatus,
        durationSeconds: e.durationSeconds,
      };
    });

    const option: GanttChartOption = {
      tooltip: {
        formatter: (params: unknown) => {
          const p = Array.isArray(params) ? params[0] : params;
          const item = p as { data: { jobName: string; jobStatus: string; durationSeconds: number } };
          const { jobName, jobStatus, durationSeconds } = item.data;
          return [
            `<b>${jobName}</b>`,
            `Status: ${jobStatus}`,
            `Duration: ${formatDuration(durationSeconds)}`,
          ].join('<br>');
        },
      },
      xAxis: { type: 'value', name: 'Offset (s)' },
      yAxis: { type: 'category', data: yAxisLabels },
      series: [
        {
          type: 'custom',
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          renderItem: (_params: unknown, api: any) => {
            const categoryIndex = api.value(0) as number;
            const startVal = api.value(1) as number;
            const endVal = api.value(2) as number;
            const start = api.coord([startVal, categoryIndex]) as number[];
            const end = api.coord([endVal, categoryIndex]) as number[];
            const height = (api.size([0, 1]) as number[])[1] * 0.6;
            return {
              type: 'rect',
              shape: {
                x: start[0],
                y: start[1] - height / 2,
                width: Math.max(end[0] - start[0], 1),
                height,
              },
              style: api.style(),
            };
          },
          encode: { x: [1, 2], y: 0 },
          data: seriesData,
        },
      ],
    };

    this.currentOption = option;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    this.chart.setOption(option as any);
  }

  getChartOption(): GanttChartOption | null {
    return this.currentOption;
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    this.resizeObserver?.disconnect();
    this.chart?.dispose();
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'ci-gantt-chart': CiGanttChart;
  }
}
