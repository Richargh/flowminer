import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { echarts } from '../echarts-setup.ts';
import type { EChartsType } from 'echarts/core';
import type { WorkItemDuration } from '../data-service.ts';

export interface MarkLineData {
  type: 'average' | 'min' | 'max';
  name?: string;
}

export interface ScatterChartOption {
  xAxis: {
    type: 'category';
    data: string[];
  };
  yAxis: {
    type: 'value';
    name?: string;
  };
  series: Array<{
    type: 'scatter';
    data: Array<[string, number]>;
    symbolSize?: number;
    markLine?: {
      data: MarkLineData[];
      label?: {
        formatter: string;
      };
    };
  }>;
  tooltip?: {
    trigger: 'item';
    formatter?: string;
  };
  grid?: {
    left: string;
    right: string;
    bottom: string;
    containLabel: boolean;
  };
}

@customElement('work-item-scatter-chart')
export class WorkItemScatterChart extends LitElement {
  private chart: EChartsType | null = null;
  private currentOption: ScatterChartOption | null = null;

  @property({ type: Array })
  data: WorkItemDuration[] = [];

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
    this.initChart();
  }

  protected updated(changedProperties: Map<string, unknown>): void {
    if (changedProperties.has('data') && this.chart) {
      this.updateChartWithData();
    }
  }

  private initChart(): void {
    const container = this.shadowRoot?.querySelector('.chart-container') as HTMLElement;
    if (!container) return;

    this.chart = echarts.init(container);
    this.updateChartWithData();
  }

  private updateChartWithData(): void {
    if (!this.chart) return;

    const dates = this.data.map(d => d.startDate);
    const scatterData = this.data.map(d => [d.startDate, d.durationDays] as [string, number]);

    const option: ScatterChartOption = {
      xAxis: {
        type: 'category',
        data: dates,
      },
      yAxis: {
        type: 'value',
        name: 'Duration (days)',
      },
      tooltip: {
        trigger: 'item',
        formatter: '{c}',
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
      },
      series: [
        {
          type: 'scatter',
          data: scatterData,
          symbolSize: 10,
          markLine: {
            data: [{ type: 'average', name: 'Average' }],
            label: {
              formatter: 'Avg: {c} days',
            },
          },
        },
      ],
    };

    this.currentOption = option;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    this.chart.setOption(option as any);
  }

  getChartOption(): ScatterChartOption | null {
    return this.currentOption;
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    this.chart?.dispose();
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'work-item-scatter-chart': WorkItemScatterChart;
  }
}
