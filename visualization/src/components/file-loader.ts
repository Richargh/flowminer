import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { parseJsonlStream } from '../mining/jsonl-parser';
import { toCommit } from '../mining/internal/commit-transform.ts';
import { GitMiner } from '../mining/git-miner';
import type {GitMiningResult} from "../mining/api-types/git-mining-result.ts";

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

    const miner = new GitMiner();

    for await (const commitDto of parseJsonlStream(file.stream())) {
      const commit = toCommit(commitDto);
      miner.process(commit);
    }

    const result = miner.getResult(new Date());

    this.filename = file.name;
    this.commitCount = result.commits.size();

    this.dispatchEvent(new CustomEvent<GitMiningResult>('data-loaded', {
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
