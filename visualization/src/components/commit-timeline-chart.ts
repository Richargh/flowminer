import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { echarts } from '../echarts-setup';
import type { EChartsType } from 'echarts/core';
import type { CommitTimeline } from '../data-service';

export interface LineChartOption {
  xAxis: {
    type: 'category';
    data: string[];
  };
  yAxis: {
    type: 'value';
  };
  series: Array<{
    type: 'line';
    data: number[];
    smooth?: boolean;
  }>;
  tooltip?: {
    trigger: 'axis';
  };
  grid?: {
    left: string;
    right: string;
    bottom: string;
    containLabel: boolean;
  };
}

@customElement('commit-timeline-chart')
export class CommitTimelineChart extends LitElement {
  private chart: EChartsType | null = null;
  private currentOption: LineChartOption | null = null;

  @property({ type: Array })
  data: CommitTimeline[] = [];

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

    const dates = this.data.map(d => d.date);
    const counts = this.data.map(d => d.cumulativeCount);

    const option: LineChartOption = {
      xAxis: {
        type: 'category',
        data: dates,
      },
      yAxis: {
        type: 'value',
      },
      tooltip: {
        trigger: 'axis',
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
      },
      series: [
        {
          type: 'line',
          data: counts,
          smooth: true,
        },
      ],
    };

    this.currentOption = option;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    this.chart.setOption(option as any);
  }

  getChartOption(): LineChartOption | null {
    return this.currentOption;
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    this.chart?.dispose();
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'commit-timeline-chart': CommitTimelineChart;
  }
}
