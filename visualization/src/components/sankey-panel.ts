import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { SankeyFlow } from '../data-service';
import './sankey-chart';

@customElement('sankey-panel')
export class SankeyPanel extends LitElement {
  @property({ type: Object })
  flow: SankeyFlow | null = null;

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
        <sankey-chart
          .data=${this.flow}
        ></sankey-chart>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'sankey-panel': SankeyPanel;
  }
}
