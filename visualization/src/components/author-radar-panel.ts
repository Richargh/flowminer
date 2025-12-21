import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import type { AuthorStats } from '../data-service';
import './author-selector';
import './author-radar-chart';

@customElement('author-radar-panel')
export class AuthorRadarPanel extends LitElement {
  @property({ type: Array })
  authors: AuthorStats[] = [];

  @state()
  private _selectedAuthor: AuthorStats | null = null;

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

  protected willUpdate(changedProperties: Map<string, unknown>): void {
    if (changedProperties.has('authors') && this.authors.length > 0 && !this._selectedAuthor) {
      this._selectedAuthor = this.authors[0];
    }
  }

  render() {
    return html`
      <div class="panel">
        <author-selector
          .authors=${this.authors}
          @author-selected=${this._onAuthorSelected}
        ></author-selector>
        <author-radar-chart
          .author=${this._selectedAuthor}
        ></author-radar-chart>
      </div>
    `;
  }

  private _onAuthorSelected(event: CustomEvent<{ author: AuthorStats }>): void {
    this._selectedAuthor = event.detail.author;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'author-radar-panel': AuthorRadarPanel;
  }
}
