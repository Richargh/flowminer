import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import type { AuthorStats } from '../data-service.ts';

const MAX_SELECTIONS = 10;
const DEFAULT_SELECTIONS = 5;

@customElement('author-selector')
export class AuthorSelector extends LitElement {
  @property({ type: Array })
  authors: AuthorStats[] = [];

  @state()
  private _selectedIndices: Set<number> = new Set();

  @state()
  private _isOpen = false;

  private _previousAuthorsLength = 0;

  static styles = css`
    :host {
      display: block;
      position: relative;
      --bg-color: #ffffff;
      --text-color: #1f2937;
      --border-color: #d1d5db;
      --hover-bg: #f3f4f6;
      --shadow-color: rgba(0, 0, 0, 0.1);
    }
    :host-context([data-theme="dark"]) {
      --bg-color: #1f2937;
      --text-color: #f9fafb;
      --border-color: #4b5563;
      --hover-bg: #374151;
      --shadow-color: rgba(0, 0, 0, 0.3);
    }
    .dropdown-trigger {
      width: 100%;
      padding: 0.5rem;
      font-size: 1rem;
      border-radius: 0.25rem;
      border: 1px solid var(--border-color);
      background: var(--bg-color);
      color: var(--text-color);
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .dropdown-trigger:hover {
      border-color: var(--text-color);
    }
    .arrow {
      transition: transform 0.2s;
    }
    .arrow.open {
      transform: rotate(180deg);
    }
    .dropdown-menu {
      position: absolute;
      top: 100%;
      left: 0;
      right: 0;
      max-height: 250px;
      overflow-y: auto;
      background: var(--bg-color);
      color: var(--text-color);
      border: 1px solid var(--border-color);
      border-top: none;
      border-radius: 0 0 0.25rem 0.25rem;
      z-index: 100;
      box-shadow: 0 4px 6px var(--shadow-color);
    }
    .dropdown-item {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.5rem;
      cursor: pointer;
    }
    .dropdown-item:hover {
      background-color: var(--hover-bg);
    }
    .dropdown-item.disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    input[type="checkbox"] {
      cursor: pointer;
    }
  `;

  /** Backward compatibility: returns first selected author */
  get selectedAuthor(): AuthorStats | undefined {
    const sortedAuthors = this._getSortedAuthors();
    for (const index of this._selectedIndices) {
      return sortedAuthors[index];
    }
    return undefined;
  }

  /** Returns all selected authors */
  get selectedAuthors(): AuthorStats[] {
    const sortedAuthors = this._getSortedAuthors();
    return Array.from(this._selectedIndices)
      .sort((a, b) => a - b)
      .map(index => sortedAuthors[index])
      .filter(Boolean);
  }

  connectedCallback(): void {
    super.connectedCallback();
    document.addEventListener('click', this._handleOutsideClick, true);
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    document.removeEventListener('click', this._handleOutsideClick, true);
  }

  private _handleOutsideClick = (e: Event): void => {
    const path = e.composedPath();
    if (!path.includes(this)) {
      this._isOpen = false;
    }
  };

  protected willUpdate(changedProperties: Map<string, unknown>): void {
    if (changedProperties.has('authors') && this.authors.length !== this._previousAuthorsLength) {
      this._previousAuthorsLength = this.authors.length;
      this._selectTopAuthors();
    }
  }

  private _getSortedAuthors(): AuthorStats[] {
    return [...this.authors].sort((a, b) => b.commitCount - a.commitCount);
  }

  private _selectTopAuthors(): void {
    this._selectedIndices = new Set();
    const count = Math.min(DEFAULT_SELECTIONS, this.authors.length);
    for (let i = 0; i < count; i++) {
      this._selectedIndices.add(i);
    }
  }

  private _getDisplayText(): string {
    const count = this._selectedIndices.size;
    if (count === 0) return 'Select authors...';
    if (count === 1) {
      const author = this.selectedAuthors[0];
      return author?.name ?? 'Select authors...';
    }
    return `${count} authors selected`;
  }

  render() {
    const sortedAuthors = this._getSortedAuthors();
    const atLimit = this._selectedIndices.size >= MAX_SELECTIONS;

    return html`
      <button
        class="dropdown-trigger"
        @click=${this._toggleDropdown}
        aria-haspopup="listbox"
        aria-expanded=${this._isOpen}
      >
        <span>${this._getDisplayText()}</span>
        <span class="arrow ${this._isOpen ? 'open' : ''}">▼</span>
      </button>
      ${this._isOpen ? html`
        <div class="dropdown-menu" role="listbox" aria-multiselectable="true">
          ${sortedAuthors.map((author, index) => {
            const isSelected = this._selectedIndices.has(index);
            const isDisabled = !isSelected && atLimit;
            return html`
              <label
                class="dropdown-item ${isDisabled ? 'disabled' : ''}"
                @click=${(e: Event) => e.stopPropagation()}
              >
                <input
                  type="checkbox"
                  .checked=${isSelected}
                  ?disabled=${isDisabled}
                  @change=${() => this._onCheckboxChange(index)}
                />
                <span>${author.name} (${author.commitCount} commits)</span>
              </label>
            `;
          })}
        </div>
      ` : ''}
    `;
  }

  private _toggleDropdown(): void {
    this._isOpen = !this._isOpen;
  }

  private _onCheckboxChange(index: number): void {
    if (this._selectedIndices.has(index)) {
      this._selectedIndices.delete(index);
    } else if (this._selectedIndices.size < MAX_SELECTIONS) {
      this._selectedIndices.add(index);
    }
    // Trigger re-render
    this._selectedIndices = new Set(this._selectedIndices);

    // Emit new event for multi-select
    this.dispatchEvent(
      new CustomEvent('authors-selected', {
        detail: { authors: this.selectedAuthors },
        bubbles: true,
        composed: true,
      })
    );

    // Also emit old event for backward compatibility
    const firstSelected = this.selectedAuthor;
    if (firstSelected) {
      this.dispatchEvent(
        new CustomEvent('author-selected', {
          detail: { author: firstSelected },
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
