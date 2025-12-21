import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { HistogramBucket } from '../data-service';
import './histogram-chart';

export interface RangeChangeDetail {
  start: number;
  end: number;
}

@customElement('histogram-panel')
export class HistogramPanel extends LitElement {
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
        <histogram-chart
          .data=${this.buckets}
          @datazoom=${this._onDataZoom}
        ></histogram-chart>
      </div>
    `;
  }

  private _onDataZoom(event: CustomEvent<{ start: number; end: number }>): void {
    this.dispatchRangeChange(event.detail.start, event.detail.end);
  }

  dispatchRangeChange(start: number, end: number): void {
    this.dispatchEvent(new CustomEvent<RangeChangeDetail>('range-change', {
      detail: { start, end },
      bubbles: true,
      composed: true,
    }));
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'histogram-panel': HistogramPanel;
  }
}
