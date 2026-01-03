import { LitElement, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { Commit } from '../commit/app/api-types/commit.ts';
import type { GitMiningResult } from '../commit-mining/app/api-types/git-mining-result.ts';
import '../elements/datatable.ts';
import type { ColumnDef } from '../elements/datatable.ts';
import { formatShortHash, formatWorkKeys, getBranchName, truncateMessage } from './commit-table-helpers.ts';

@customElement('commits-table-panel')
export class CommitsTablePanel extends LitElement {
  @property({ type: Array })
  commits: Commit[] = [];

  private get columns(): ColumnDef<Commit>[] {
    const uniqueAuthors = [...new Set(this.commits.map(c => c.author.name))].sort();
    const uniqueTypes = [...new Set(this.commits.map(c => c.commitType))].sort();
    const uniqueWorkKeys = [...new Set(this.commits.flatMap(c => formatWorkKeys(c.workKeys).split(', ').filter(k => k)))].sort();
    const uniqueBranches = [...new Set(this.commits.map(c => getBranchName(c.branchId)).filter(b => b))].sort();

    return [
      { id: 'hash', header: 'Hash', accessor: (c) => formatShortHash(c.hash), sortable: true },
      { id: 'author', header: 'Author', accessor: (c) => c.author.name, sortable: true, filterable: true, filterType: 'select', filterOptions: uniqueAuthors },
      { id: 'date', header: 'Date', accessor: (c) => c.date.toLocaleDateString(), sortable: true },
      { id: 'message', header: 'Message', accessor: (c) => truncateMessage(c.message), sortable: true },
      { id: 'type', header: 'Type', accessor: (c) => c.commitType, sortable: true, filterable: true, filterType: 'multiSelect', filterOptions: uniqueTypes },
      { id: 'workKeys', header: 'Work Keys', accessor: (c) => formatWorkKeys(c.workKeys), sortable: true, filterable: true, filterType: 'select', filterOptions: uniqueWorkKeys },
      { id: 'branch', header: 'Branch', accessor: (c) => getBranchName(c.branchId), sortable: true, filterable: true, filterType: 'select', filterOptions: uniqueBranches },
    ];
  }

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

  // Use light DOM so DaisyUI styles apply to child data-table
  createRenderRoot() {
    return this;
  }

  render() {
    return html`
      <data-table
        .data=${this.commits}
        .columns=${this.columns}
        .emptyMessage=${'No commits in selected range'}
        .defaultSortColumnId=${'date'}
        .defaultSortDirection=${'desc'}
        .maxVisibleRows=${10}
      ></data-table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'commits-table-panel': CommitsTablePanel;
  }
}
