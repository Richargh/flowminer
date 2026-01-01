import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { echarts } from '../echarts-setup.ts';
import type { EChartsType } from 'echarts/core';
import type { HistogramBucket } from '../data-service.ts';

export interface WorkItemDurationChartOption {
  xAxis: {
    type: 'category';
    data: string[];
  };
  yAxis: {
    type: 'value';
    name?: string;
  };
  series: Array<{
    type: 'bar';
    data: number[];
  }>;
  tooltip?: {
    trigger: 'axis';
    axisPointer?: {
      type: 'shadow';
    };
  };
  grid?: {
    left: string;
    right: string;
    bottom: string;
    containLabel: boolean;
  };
}

@customElement('work-item-duration-chart')
export class WorkItemDurationChart extends LitElement {
  private chart: EChartsType | null = null;
  private currentOption: WorkItemDurationChartOption | null = null;

  @property({ type: Array })
  data: HistogramBucket[] = [];

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

    const ranges = this.data.map(d => d.range);
    const counts = this.data.map(d => d.count);

    const option: WorkItemDurationChartOption = {
      xAxis: {
        type: 'category',
        data: ranges,
      },
      yAxis: {
        type: 'value',
        name: 'Count',
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
        },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '15%',
        containLabel: true,
      },
      series: [
        {
          type: 'bar',
          data: counts,
        },
      ],
    };

    this.currentOption = option;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    this.chart.setOption(option as any);
  }

  getChartOption(): WorkItemDurationChartOption | null {
    return this.currentOption;
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    this.chart?.dispose();
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'work-item-duration-chart': WorkItemDurationChart;
  }
}
