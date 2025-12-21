import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { CommitTimeline } from '../data-service';
import './commit-timeline-chart';

@customElement('commit-timeline-panel')
export class CommitTimelinePanel extends LitElement {
  @property({ type: Array })
  timeline: CommitTimeline[] = [];

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
        <commit-timeline-chart
          .data=${this.timeline}
        ></commit-timeline-chart>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'commit-timeline-panel': CommitTimelinePanel;
  }
}
