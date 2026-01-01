import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { echarts } from '../echarts-setup.ts';
import type { EChartsType } from 'echarts/core';
import type { AuthorStats } from '../data-service.ts';

export interface RadarChartOption {
  radar: {
    indicator: Array<{ name: string; max: number }>;
    axisName?: {
      color: string;
    };
    splitLine?: {
      lineStyle: { color: string };
    };
    splitArea?: {
      areaStyle: { color: string[] };
    };
  };
  legend?: {
    data: string[];
    textStyle?: { color: string };
  };
  series: Array<{
    type: 'radar';
    data: Array<{
      value: number[];
      name: string;
      itemStyle?: { color: string };
      lineStyle?: { color: string };
      areaStyle?: { color: string; opacity: number };
    }>;
  }>;
}

// Color palette for multi-series radar chart (10 distinct colors)
const AUTHOR_COLORS = [
  '#5470c6', // blue
  '#91cc75', // green
  '#fac858', // yellow
  '#ee6666', // red
  '#73c0de', // light blue
  '#3ba272', // dark green
  '#fc8452', // orange
  '#9a60b4', // purple
  '#ea7ccc', // pink
  '#48b8b8', // teal
];

function getAuthorColor(index: number): string {
  return AUTHOR_COLORS[index % AUTHOR_COLORS.length];
}

@customElement('author-radar-chart')
export class AuthorRadarChart extends LitElement {
  private chart: EChartsType | null = null;
  private currentOption: RadarChartOption | null = null;
  private themeObserver: MutationObserver | null = null;

  @property({ type: Object })
  author: AuthorStats | null = null;

  @property({ type: Array })
  authors: AuthorStats[] = [];

  @state()
  private isDarkMode = false;

  static styles = css`
    :host {
      display: block;
    }
    .chart-container {
      width: 100%;
      height: 400px;
    }
  `;

  private get textColor(): string {
    return this.isDarkMode ? '#f9fafb' : '#1f2937';
  }

  private get gridColor(): string {
    return this.isDarkMode ? '#374151' : '#e5e7eb';
  }

  render() {
    return html`<div class="chart-container"></div>`;
  }

  connectedCallback(): void {
    super.connectedCallback();
    this.isDarkMode = this.detectDarkMode();

    // Watch for theme changes
    this.themeObserver = new MutationObserver(() => {
      const newIsDark = this.detectDarkMode();
      if (newIsDark !== this.isDarkMode) {
        this.isDarkMode = newIsDark;
        this.updateChartWithAuthor();
      }
    });
    this.themeObserver.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['data-theme'],
    });

    // Also watch for system theme changes when no explicit data-theme is set
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      const newIsDark = this.detectDarkMode();
      if (newIsDark !== this.isDarkMode) {
        this.isDarkMode = newIsDark;
        this.updateChartWithAuthor();
      }
    });
  }

  private detectDarkMode(): boolean {
    const dataTheme = document.documentElement.getAttribute('data-theme');
    if (dataTheme) {
      return dataTheme === 'dark';
    }
    // Fall back to system preference if no explicit theme is set
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    this.themeObserver?.disconnect();
    this.chart?.dispose();
  }

  protected firstUpdated(): void {
    this.initChart();
  }

  protected updated(changedProperties: Map<string, unknown>): void {
    if ((changedProperties.has('author') || changedProperties.has('authors')) && this.chart) {
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

    // Use authors array if available, otherwise fall back to single author
    const authorsToDisplay = this.authors.length > 0 ? this.authors : this.author ? [this.author] : [];

    // Compute max values for normalization (use at least 1 to avoid division by zero)
    const maxCommitCount = Math.max(1, ...authorsToDisplay.map(a => a.commitCount));
    const maxLinesAdded = Math.max(1, ...authorsToDisplay.map(a => a.linesAdded));
    const maxLinesDeleted = Math.max(1, ...authorsToDisplay.map(a => a.linesDeleted));
    const maxAvgCommitSize = Math.max(1, ...authorsToDisplay.map(a => a.avgCommitSize));

    const seriesData = authorsToDisplay.length > 0
      ? authorsToDisplay.map((author, index) => ({
          value: [author.commitCount, author.linesAdded, author.linesDeleted, author.avgCommitSize],
          name: author.name,
          itemStyle: { color: getAuthorColor(index) },
          lineStyle: { color: getAuthorColor(index) },
          areaStyle: { color: getAuthorColor(index), opacity: 0.2 },
        }))
      : [{ value: [0, 0, 0, 0], name: 'No Author' }];

    const option: RadarChartOption = {
      radar: {
        indicator: [
          { name: 'Commit Count', max: maxCommitCount },
          { name: 'Lines Added', max: maxLinesAdded },
          { name: 'Lines Deleted', max: maxLinesDeleted },
          { name: 'Avg Commit Size', max: maxAvgCommitSize },
        ],
        axisName: {
          color: this.textColor,
        },
        splitLine: {
          lineStyle: { color: this.gridColor },
        },
        splitArea: {
          areaStyle: { color: ['transparent', 'transparent'] },
        },
      },
      legend: {
        data: authorsToDisplay.map(author => author.name),
        textStyle: { color: this.textColor },
      },
      series: [
        {
          type: 'radar',
          data: seriesData,
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
}

declare global {
  interface HTMLElementTagNameMap {
    'author-radar-chart': AuthorRadarChart;
  }
}
