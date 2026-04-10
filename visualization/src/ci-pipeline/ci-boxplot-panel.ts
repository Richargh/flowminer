import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { PipelineJob, BoxplotEntry } from './ci-types.ts';
import type { CiMiningResult } from './ci-file-loader.ts';
import './ci-boxplot-chart.ts';

function computeBoxplotData(jobs: PipelineJob[]): BoxplotEntry[] {
  const byJobName = new Map<string, number[]>();

  for (const job of jobs) {
    if (job.jobDurationSeconds == null) continue;
    const existing = byJobName.get(job.jobName) ?? [];
    existing.push(job.jobDurationSeconds);
    byJobName.set(job.jobName, existing);
  }

  const entries: BoxplotEntry[] = [];
  for (const [jobName, durations] of byJobName) {
    if (durations.length === 0) continue;
    const sorted = [...durations].sort((a, b) => a - b);
    const n = sorted.length;
    const min = sorted[0];
    const max = sorted[n - 1];
    const median = n % 2 === 0
      ? (sorted[n / 2 - 1] + sorted[n / 2]) / 2
      : sorted[Math.floor(n / 2)];
    const q1 = sorted[Math.floor(n / 4)];
    const q3 = sorted[Math.floor((3 * n) / 4)];
    entries.push({ jobName, values: [min, q1, median, q3, max] });
  }
  return entries;
}

@customElement('ci-boxplot-panel')
export class CiBoxplotPanel extends LitElement {
  @property({ type: Array })
  boxplotData: BoxplotEntry[] = [];

  private handleCiDataLoaded = (event: Event): void => {
    const customEvent = event as CustomEvent<CiMiningResult>;
    this.boxplotData = computeBoxplotData(customEvent.detail.jobs);
  };

  connectedCallback(): void {
    super.connectedCallback();
    document.addEventListener('ci-data-loaded', this.handleCiDataLoaded);
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    document.removeEventListener('ci-data-loaded', this.handleCiDataLoaded);
  }

  static styles = css`
    :host {
      display: block;
    }
    .empty-state {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 300px;
      color: var(--text-secondary, #666);
      font-style: italic;
    }
  `;

  render() {
    if (this.boxplotData.length === 0) {
      return html`<div class="empty-state">No CI pipeline data loaded</div>`;
    }

    return html`<ci-boxplot-chart .data=${this.boxplotData}></ci-boxplot-chart>`;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'ci-boxplot-panel': CiBoxplotPanel;
  }
}
