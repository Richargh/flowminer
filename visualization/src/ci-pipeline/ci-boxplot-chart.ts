import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { echarts } from '../echarts-setup.ts';
import type { EChartsType } from 'echarts/core';
import type { BoxplotEntry } from './ci-types.ts';
import { truncate, MAX_LABEL_LENGTH, formatDuration } from './ci-chart-utils.ts';

export interface BoxplotChartOption {
  tooltip: { formatter: (params: unknown) => string };
  xAxis: { type: 'value'; name: string };
  yAxis: { type: 'category'; data: string[] };
  series: Array<{
    type: 'boxplot';
    data: [number, number, number, number, number][];
  }>;
}

@customElement('ci-boxplot-chart')
export class CiBoxplotChart extends LitElement {
  private chart: EChartsType | null = null;
  private currentOption: BoxplotChartOption | null = null;
  private resizeObserver: ResizeObserver | null = null;

  @property({ type: Array })
  data: BoxplotEntry[] = [];

  static styles = css`
    :host {
      display: block;
    }
    .chart-container {
      width: 100%;
      height: 400px;
    }
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

  private updateChartWithData(): void {
    if (!this.chart) return;

    const entries = this.data;
    const yAxisLabels = entries.map(d => truncate(d.jobName, MAX_LABEL_LENGTH));
    const boxplotData = entries.map(d => d.values);

    const option: BoxplotChartOption = {
      tooltip: {
        formatter: (params: unknown) => {
          const p = Array.isArray(params) ? params[0] : params;
          const item = p as { dataIndex: number; data: [number, number, number, number, number] };
          const entry = entries[item.dataIndex];
          const [min, q1, median, q3, max] = item.data;
          return [
            `<b>${entry.jobName}</b>`,
            `Min: ${formatDuration(min)}`,
            `Q1: ${formatDuration(q1)}`,
            `Median: ${formatDuration(median)}`,
            `Q3: ${formatDuration(q3)}`,
            `Max: ${formatDuration(max)}`
          ].join('<br>');
        }
      },
      xAxis: { type: 'value', name: 'Duration' },
      yAxis: { type: 'category', data: yAxisLabels },
      series: [
        {
          type: 'boxplot',
          data: boxplotData
        }
      ]
    };

    this.currentOption = option;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    this.chart.setOption(option as any);
  }

  getChartOption(): BoxplotChartOption | null {
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
    'ci-boxplot-chart': CiBoxplotChart;
  }
}
