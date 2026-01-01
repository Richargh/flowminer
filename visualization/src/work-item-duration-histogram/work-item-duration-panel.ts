import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { HistogramBucket } from '../data-service.ts';
import type { GitMiningResult } from '../commit-mining/app/api-types/git-mining-result.ts';
import './work-item-duration-chart.ts';

const BUCKET_RANGES = [
  { range: '0-2 days', minValue: 0, maxValue: 2 },
  { range: '2-4 days', minValue: 2, maxValue: 4 },
  { range: '4-6 days', minValue: 4, maxValue: 6 },
  { range: '6-8 days', minValue: 6, maxValue: 8 },
  { range: '8-10 days', minValue: 8, maxValue: 10 },
  { range: '10+ days', minValue: 10, maxValue: 999 },
];

@customElement('work-item-duration-panel')
export class WorkItemDurationPanel extends LitElement {
  @property({ type: Array })
  buckets: HistogramBucket[] = [];

  private handleDataLoaded = (event: Event): void => {
    const customEvent = event as CustomEvent<GitMiningResult>;
    const result = customEvent.detail;

    const durations = result.workItems.all()
      .filter(wi => wi.workKey.type === 'known')
      .map(wi => {
        const durationMs = wi.lastCommitDate.getTime() - wi.firstCommitDate.getTime();
        return Math.max(1, Math.round(durationMs / (1000 * 60 * 60 * 24)));
      });

    this.buckets = BUCKET_RANGES.map(bucket => ({
      ...bucket,
      count: durations.filter(d => d >= bucket.minValue && d < bucket.maxValue).length
    }));
  };

  connectedCallback(): void {
    super.connectedCallback();
    document.addEventListener('data-loaded', this.handleDataLoaded);
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    document.removeEventListener('data-loaded', this.handleDataLoaded);
  }

  static styles = css`
    :host {
      display: block;
    }
    .panel {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
  `;

  render() {
    return html`
      <div class="panel">
        <work-item-duration-chart
          .data=${this.buckets}
        ></work-item-duration-chart>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'work-item-duration-panel': WorkItemDurationPanel;
  }
}
