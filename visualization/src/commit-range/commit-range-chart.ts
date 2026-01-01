import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { echarts } from '../echarts-setup.ts';
import type { EChartsType } from 'echarts/core';

import type {CommitActivity} from "./app/internal/commit-activity.ts";

interface CommitRangeChartOption {
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
  dataZoom: Array<{
    type: 'slider' | 'inside';
    start?: number;
    end?: number;
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
  brush?: {
    xAxisIndex: number;
    brushLink?: string;
    brushType?: 'lineX' | 'rect' | 'polygon' | 'keep' | 'clear';
    outOfBrush?: {
      colorAlpha: number;
    };
    brushStyle?: {
      borderWidth: number;
      color: string;
      borderColor: string;
    };
  };
}

@customElement('commit-range-chart')
export class CommitRangeChart extends LitElement {
  private chart: EChartsType | null = null;
  private currentOption: CommitRangeChartOption | null = null;

  @property({ type: Array })
  data: CommitActivity[] = [];

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
    this.chart.on('dataZoom', () => {
      this.emitTimeRangeChanged();
    });
    this.chart.on('brushEnd', (params: unknown) => {
      this.handleBrushEnd(params);
    });
    this.updateChartWithData();
  }

  private updateChartWithData(): void {
    if (!this.chart) return;

    const dates = this.data.map(d => d.date);
    const counts = this.data.map(d => d.count);

    const option: CommitRangeChartOption = {
      xAxis: {
        type: 'category',
        data: dates,
      },
      yAxis: {
        type: 'value',
        name: 'Commits',
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
      dataZoom: [
        {
          type: 'slider',
          start: 0,
          end: 100,
        },
        {
          type: 'inside',
          start: 0,
          end: 100,
        },
      ],
      brush: {
        xAxisIndex: 0,
        brushLink: 'all',
        brushType: 'lineX',
        outOfBrush: {
          colorAlpha: 0.3,
        },
        brushStyle: {
          borderWidth: 1,
          color: 'rgba(59, 130, 246, 0.2)',
          borderColor: 'rgba(59, 130, 246, 0.8)',
        },
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

    // Activate brush mode by default
    this.chart.dispatchAction({
      type: 'takeGlobalCursor',
      key: 'brush',
      brushOption: {
        brushType: 'lineX',
      },
    });
  }

  getChartOption(): CommitRangeChartOption | null {
    return this.currentOption;
  }

  private emitTimeRangeChanged(startPercent: number = 0, endPercent: number = 100): void {
    if (this.data.length === 0) return;

    const maxIndex = this.data.length - 1;
    const startIndex = Math.round((startPercent / 100) * maxIndex);
    const endIndex = Math.round((endPercent / 100) * maxIndex);

    const startDate = this.data[startIndex]?.date;
    const endDate = this.data[endIndex]?.date;

    if (startDate && endDate) {
      this.dispatchEvent(new CustomEvent('time-range-changed', {
        detail: { startDate, endDate },
        bubbles: true,
        composed: true,
      }));
    }
  }

  simulateDataZoomEvent(params: { start: number; end: number }): void {
    this.emitTimeRangeChanged(params.start, params.end);
  }

  private handleBrushEnd(params: unknown): void {
    const brushParams = params as {
      areas?: Array<{
        coordRange?: [number, number];
      }>;
    };

    const coordRange = brushParams.areas?.[0]?.coordRange;
    if (!coordRange || !this.chart || this.data.length === 0) return;

    const [startIndex, endIndex] = coordRange;
    if (startIndex === endIndex) return; // No range selected

    // Calculate percentage range for dataZoom
    const maxIndex = this.data.length - 1;
    const startPercent = (Math.min(startIndex, endIndex) / maxIndex) * 100;
    const endPercent = (Math.max(startIndex, endIndex) / maxIndex) * 100;

    // Clear the brush selection
    this.chart.dispatchAction({
      type: 'brush',
      areas: [],
    });

    // Zoom to the selected range
    this.chart.dispatchAction({
      type: 'dataZoom',
      start: startPercent,
      end: endPercent,
    });

    this.emitTimeRangeChangedByIndex(Math.min(startIndex, endIndex), Math.max(startIndex, endIndex));
  }

  private emitTimeRangeChangedByIndex(startIndex: number, endIndex: number): void {
    const startDate = this.data[startIndex]?.date;
    const endDate = this.data[endIndex]?.date;

    if (startDate && endDate) {
      this.dispatchEvent(new CustomEvent('time-range-changed', {
        detail: { startDate, endDate },
        bubbles: true,
        composed: true,
      }));
    }
  }

  simulateBrushEvent(params: { startIndex: number; endIndex: number }): void {
    this.emitTimeRangeChangedByIndex(params.startIndex, params.endIndex);
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    this.chart?.dispose();
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'commit-range-chart': CommitRangeChart;
  }
}
