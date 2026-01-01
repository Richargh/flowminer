import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import type { AuthorStats } from '../data-service.ts';
import type { AuthorSelector } from './author-selector.ts';
import './author-selector.ts';
import './author-radar-chart.ts';

@customElement('author-radar-panel')
export class AuthorRadarPanel extends LitElement {
  @property({ type: Array })
  authors: AuthorStats[] = [];

  @state()
  private _selectedAuthors: AuthorStats[] = [];

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

  protected async firstUpdated(): Promise<void> {
    // Wait for selector to initialize and get its default selection
    await this.updateComplete;
    const selector = this.shadowRoot?.querySelector('author-selector') as AuthorSelector;
    if (selector) {
      await selector.updateComplete;
      this._selectedAuthors = selector.selectedAuthors;
    }
  }

  protected willUpdate(changedProperties: Map<string, unknown>): void {
    if (changedProperties.has('authors') && this.authors.length > 0) {
      // Selector will auto-select top 5, we'll get them via event
      // For initial load, we need to trigger this after selector updates
      this._scheduleSelectionSync();
    }
  }

  private async _scheduleSelectionSync(): Promise<void> {
    await this.updateComplete;
    const selector = this.shadowRoot?.querySelector('author-selector') as AuthorSelector;
    if (selector) {
      await selector.updateComplete;
      this._selectedAuthors = selector.selectedAuthors;
    }
  }

  render() {
    return html`
      <div class="panel">
        <author-selector
          .authors=${this.authors}
          @authors-selected=${this._onAuthorsSelected}
        ></author-selector>
        <author-radar-chart
          .authors=${this._selectedAuthors}
        ></author-radar-chart>
      </div>
    `;
  }

  private _onAuthorsSelected(event: CustomEvent<{ authors: AuthorStats[] }>): void {
    this._selectedAuthors = event.detail.authors;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'author-radar-panel': AuthorRadarPanel;
  }
}
