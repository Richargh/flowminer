import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { WorkItemDuration } from '../data-service';
import './work-item-scatter-chart';

@customElement('work-item-scatter-panel')
export class WorkItemScatterPanel extends LitElement {
  @property({ type: Array })
  workItems: WorkItemDuration[] = [];

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
