import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { echarts } from '../echarts-setup.ts';
import type { EChartsType } from 'echarts/core';
import type { AuthorStats } from '../data-service.ts';

export interface RadarChartOption {
  radar: {
    indicator: Array<{ name: string; max: number }>;
  };
  series: Array<{
    type: 'radar';
    data: Array<{
      value: number[];
      name: string;
    }>;
  }>;
}

@customElement('author-radar-chart')
export class AuthorRadarChart extends LitElement {
  private chart: EChartsType | null = null;
  private currentOption: RadarChartOption | null = null;

  @property({ type: Object })
  author: AuthorStats | null = null;

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
    if (changedProperties.has('author') && this.chart) {
      this.updateChartWithAuthor();
    }
  }

  private initChart(): void {
    const container = this.shadowRoot?.querySelector('.chart-container') as HTMLElement;
    if (!container) return;

    this.chart = echarts.init(container);
    this.updateChartWithAuthor();
  }

  private updateChartWithAuthor(): void {
    if (!this.chart) return;

    const author = this.author;
    const option: RadarChartOption = {
      radar: {
        indicator: [
          { name: 'Commit Count', max: 200 },
          { name: 'Lines Added', max: 10000 },
          { name: 'Lines Deleted', max: 5000 },
          { name: 'Avg Commit Size', max: 200 },
        ],
      },
      series: [
        {
          type: 'radar',
          data: [
            {
              value: author
                ? [author.commitCount, author.linesAdded, author.linesDeleted, author.avgCommitSize]
                : [0, 0, 0, 0],
              name: author?.name ?? 'No Author',
            },
          ],
        },
      ],
    };

    this.currentOption = option;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    this.chart.setOption(option as any);
  }

  getChartOption(): RadarChartOption | null {
    return this.currentOption;
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    this.chart?.dispose();
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'author-radar-chart': AuthorRadarChart;
  }
}
