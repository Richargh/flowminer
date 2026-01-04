import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { WorkItemDuration } from '../data-service.ts';
import type { GitMiningResult } from '../commit-mining/app/api-types/git-mining-result.ts';
import './work-item-scatter-chart.ts';

@customElement('work-item-scatter-panel')
export class WorkItemScatterPanel extends LitElement {
  @property({ type: Array })
  workItems: WorkItemDuration[] = [];

  private handleDataLoaded = (event: Event): void => {
    const customEvent = event as CustomEvent<GitMiningResult>;
    const result = customEvent.detail;

    this.workItems = result.workItems.all()
      .filter(wi => wi.workKey.type === 'known')
      .map(wi => {
        const durationMs = wi.lastCommitDate - wi.firstCommitDate;
        const durationDays = Math.max(1, Math.round(durationMs / (1000 * 60 * 60 * 24)));
        let maxType = 'Feature';
        let maxChurn = 0;
        for (const [type, churn] of wi.absoluteChurnByType) {
          if (churn > maxChurn) {
            maxChurn = churn;
            maxType = type;
          }
        }
        return {
          key: (wi.workKey as { type: 'known'; key: string }).key,
          type: maxType,
          startDate: new Date(wi.firstCommitDate).toISOString().split('T')[0],
          durationDays
        };
      });
  };

  connectedCallback(): void {
    super.connectedCallback();
    document.addEventListener('data-loaded', this.handleDataLoaded);
    document.addEventListener('data-constrained', this.handleDataLoaded);
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    document.removeEventListener('data-loaded', this.handleDataLoaded);
    document.removeEventListener('data-constrained', this.handleDataLoaded);
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
    if (this.workItems.length === 0) {
      return html`
        <div class="panel">
          <div class="empty-state">No work items in selected range</div>
        </div>
      `;
    }

    return html`
      <div class="panel">
        <work-item-scatter-chart
          .data=${this.workItems}
        ></work-item-scatter-chart>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'work-item-scatter-panel': WorkItemScatterPanel;
  }
}
