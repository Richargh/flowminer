import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import './commit-range-chart.ts';
import type {CommitActivity} from "./app/internal/commit-activity.ts";
import { aggregateCommitsByDate } from './app/internal/commit-activity.ts';
import type { GitMiningResult } from '../commit-mining/app/api-types/git-mining-result.ts';

@customElement('commit-range-panel')
export class CommitRangePanel extends LitElement {
  @property({ type: Array })
  data: CommitActivity[] = [];

  private handleDataLoaded = (event: Event): void => {
    const customEvent = event as CustomEvent<GitMiningResult>;
    this.data = aggregateCommitsByDate(customEvent.detail.commits.all());
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
        <commit-range-chart
          .data=${this.data}
        ></commit-range-chart>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'commit-range-panel': CommitRangePanel;
  }
}
