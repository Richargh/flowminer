import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { Commit } from '../commit/app/api-types/commit.ts';
import type { GitMiningResult } from '../commit-mining/app/api-types/git-mining-result.ts';
import '../elements/datatable.ts';
import type { ColumnDef } from '../elements/datatable.ts';

@customElement('commits-table-panel')
export class CommitsTablePanel extends LitElement {
  @property({ type: Array })
  commits: Commit[] = [];

  private columns: ColumnDef<Commit>[] = [
    { id: 'hash', header: 'Hash', accessor: (c) => c.hash.slice(0, 7) },
    { id: 'author', header: 'Author', accessor: (c) => c.author.name },
    { id: 'date', header: 'Date', accessor: (c) => c.date.toLocaleDateString() },
    { id: 'message', header: 'Message', accessor: (c) => c.message },
  ];

  private handleDataLoaded = (event: Event): void => {
    const customEvent = event as CustomEvent<GitMiningResult>;
    const result = customEvent.detail;
    this.commits = result.commits.all();
  };

  connectedCallback(): void {
    super.connectedCallback();
    document.addEventListener('data-loaded', this.handleDataLoaded);
    document.addEventListener('data-constrained', this.handleDataLoaded);
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    document.removeEventListener('data-loaded', this.handleDataLoaded);
    document.removeEventListener('data-constrained', this.handleDataLoaded);
  }

  static styles = css`
    :host {
      display: block;
    }
  `;

  render() {
    return html`
      <data-table
        .data=${this.commits}
        .columns=${this.columns}
      ></data-table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'commits-table-panel': CommitsTablePanel;
  }
}
