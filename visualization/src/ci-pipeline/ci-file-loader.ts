import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import type { PipelineJob } from './ci-types.ts';

export interface CiMiningResult {
  jobs: PipelineJob[];
}

function parseJobsFromJsonl(jsonlContent: string): CiMiningResult {
  const jobs: PipelineJob[] = [];
  for (const line of jsonlContent.split('\n')) {
    if (line.trim().length === 0) continue;
    try {
      jobs.push(JSON.parse(line) as PipelineJob);
    } catch {
      console.warn('Skipping malformed JSONL line:', line);
    }
  }
  return { jobs };
}

@customElement('ci-file-loader')
export class CiFileLoader extends LitElement {
  @state()
  private filename: string | null = null;

  @state()
  private jobCount: number = 0;

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
      <button class="btn btn-secondary" @click=${this._onButtonClick}>
        Load CI JSONL
      </button>
      ${this.filename ? html`
        <span class="file-info">${this.filename} (${this.jobCount} jobs)</span>
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

    const text = await file.text();
    const result = parseJobsFromJsonl(text);

    this.filename = file.name;
    this.jobCount = result.jobs.length;

    document.dispatchEvent(new CustomEvent<CiMiningResult>('ci-data-loaded', {
      detail: result,
      bubbles: true,
      composed: true
    }));
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'ci-file-loader': CiFileLoader;
  }
}
