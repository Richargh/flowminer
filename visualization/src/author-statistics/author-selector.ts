import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import type { AuthorStats } from '../data-service.ts';

@customElement('author-selector')
export class AuthorSelector extends LitElement {
  @property({ type: Array })
  authors: AuthorStats[] = [];

  @state()
  private _selectedIndex = 0;

  static styles = css`
    :host {
      display: block;
    }
    select {
      width: 100%;
      padding: 0.5rem;
      font-size: 1rem;
      border-radius: 0.25rem;
      border: 1px solid #ccc;
    }
  `;

  get selectedAuthor(): AuthorStats | undefined {
    return this.authors[this._selectedIndex];
  }

  render() {
    return html`
      <select aria-label="Select author" @change=${this._onSelectionChange}>
        ${this.authors.map(
          (author, index) =>
            html`<option value=${author.name} ?selected=${index === this._selectedIndex}>
              ${author.name}
            </option>`
        )}
      </select>
    `;
  }

  private _onSelectionChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const selectedName = select.value;
    const index = this.authors.findIndex((a) => a.name === selectedName);

    if (index !== -1) {
      this._selectedIndex = index;
      this.dispatchEvent(
        new CustomEvent('author-selected', {
          detail: { author: this.authors[index] },
          bubbles: true,
          composed: true,
        })
      );
    }
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'author-selector': AuthorSelector;
  }
}
