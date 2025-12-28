import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { mineCommitsFromJsonl, type GitMiningResultDto } from 'teamcharta-git-importer';

@customElement('file-loader')
export class FileLoader extends LitElement {
  @state()
  private filename: string | null = null;

  @state()
  private commitCount: number = 0;

  static styles = css`
    :host {
      display: inline-flex;
      align-items: center;
      gap: 0.75rem;
    }
    input[type="file"] {
      display: none;
    }
    .file-info {
      font-size: 0.875rem;
      opacity: 0.8;
    }
  `;

  render() {
    return html`
      <input
        type="file"
        accept=".jsonl"
        @change=${this._onFileChange}
      />
      <button class="btn btn-primary" @click=${this._onButtonClick}>
        Load JSONL
      </button>
      ${this.filename ? html`
        <span class="file-info">${this.filename} (${this.commitCount} commits)</span>
      ` : ''}
    `;
  }

  private _onButtonClick(): void {
    const input = this.shadowRoot?.querySelector('input[type="file"]') as HTMLInputElement;
    input?.click();
  }

  private async _onFileChange(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const content = await file.text();
    const result = mineCommitsFromJsonl(content);

    this.filename = file.name;
    this.commitCount = result.commitTimeline.asJsReadonlyArrayView().length;

    this.dispatchEvent(new CustomEvent<GitMiningResultDto>('data-loaded', {
      detail: result,
      bubbles: true,
      composed: true
    }));
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'file-loader': FileLoader;
  }
}
