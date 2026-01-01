import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { HistogramBucket } from '../data-service.ts';
import './work-item-duration-chart.ts';

@customElement('work-item-duration-panel')
export class WorkItemDurationPanel extends LitElement {
  @property({ type: Array })
  buckets: HistogramBucket[] = [];

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
