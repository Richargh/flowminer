import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';

@customElement('theme-switcher')
export class ThemeSwitcher extends LitElement {
  @state()
  private isDark = false;

  static styles = css`
    :host {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
    }
    .theme-label {
      font-size: 0.875rem;
      opacity: 0.7;
    }
    .theme-label.active {
      opacity: 1;
      font-weight: 500;
    }
  `;

  connectedCallback(): void {
    super.connectedCallback();
    this.isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  }

  render() {
    return html`
      <span class="theme-label ${!this.isDark ? 'active' : ''}">Light</span>
      <input
        type="checkbox"
        class="toggle"
        aria-label="Toggle dark mode"
        .checked=${this.isDark}
        @change=${this._onToggle}
      />
      <span class="theme-label ${this.isDark ? 'active' : ''}">Dark</span>
    `;
  }

  private _onToggle(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.isDark = target.checked;
    document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'theme-switcher': ThemeSwitcher;
  }
}
