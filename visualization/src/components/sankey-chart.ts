import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { echarts } from '../echarts-setup';
import type { EChartsType } from 'echarts/core';
import type { SankeyFlow, SankeyNode, SankeyLink } from '../data-service';

export interface SankeyChartOption {
  series: Array<{
    type: 'sankey';
    data: SankeyNode[];
    links: SankeyLink[];
    emphasis?: {
      focus: 'adjacency';
    };
    lineStyle?: {
      color: 'gradient';
      curveness: number;
    };
  }>;
  tooltip?: {
    trigger: 'item';
    triggerOn: 'mousemove';
  };
}

@customElement('sankey-chart')
export class SankeyChart extends LitElement {
  private chart: EChartsType | null = null;
  private currentOption: SankeyChartOption | null = null;

  @property({ type: Object })
  data: SankeyFlow | null = null;

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

    const nodes = this.data?.nodes ?? [];
    const links = this.data?.links ?? [];

    const option: SankeyChartOption = {
      tooltip: {
        trigger: 'item',
        triggerOn: 'mousemove',
      },
      series: [
        {
          type: 'sankey',
          data: nodes,
          links: links,
          emphasis: {
            focus: 'adjacency',
          },
          lineStyle: {
            color: 'gradient',
            curveness: 0.5,
          },
        },
      ],
    };

    this.currentOption = option;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    this.chart.setOption(option as any);
  }

  getChartOption(): SankeyChartOption | null {
    return this.currentOption;
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    this.chart?.dispose();
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'sankey-chart': SankeyChart;
  }
}
