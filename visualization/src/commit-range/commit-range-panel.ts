import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import './commit-range-chart.ts';
import type {CommitActivity} from "./app/internal/commit-activity.ts";

@customElement('commit-range-panel')
export class CommitRangePanel extends LitElement {
  @property({ type: Array })
  data: CommitActivity[] = [];

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
