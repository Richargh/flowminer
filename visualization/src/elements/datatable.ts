import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

export interface ColumnDef<T> {
  id: string;
  header: string;
  accessor: (row: T) => unknown;
  sortable?: boolean;
  searchable?: boolean;
  filterable?: boolean;
  filterType?: 'select';
  filterOptions?: string[];
}

type SortDirection = 'asc' | 'desc' | null;

@customElement('data-table')
export class DataTable<T> extends LitElement {
  @property({ type: Array })
  data: T[] = [];

  @property({ type: Array })
  columns: ColumnDef<T>[] = [];

  @property({ type: String })
  emptyMessage = '';

  @property({ type: String, attribute: 'default-sort-column' })
  defaultSortColumnId: string | null = null;

  @property({ type: String, attribute: 'default-sort-direction' })
  defaultSortDirection: SortDirection = null;

  @property({ type: Number, attribute: 'max-visible-rows' })
  maxVisibleRows: number | null = null;

  @state()
  private sortColumnId: string | null = null;

  @state()
  private sortDirection: SortDirection = null;

  @state()
  private initialized = false;

  @state()
  private searchTerm = '';

  @state()
  private columnFilters: Record<string, string> = {};

  static styles = css`
    :host {
      display: block;
    }
    th {
      cursor: pointer;
    }
    .table-scroll-container {
      overflow-y: auto;
    }
  `;

  protected willUpdate(): void {
    if (!this.initialized && this.defaultSortColumnId) {
      this.sortColumnId = this.defaultSortColumnId;
      this.sortDirection = this.defaultSortDirection || 'asc';
      this.initialized = true;
    }
  }

  private handleSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm = input.value;
  }

  private handleColumnFilterChange(colId: string, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const value = select.value;
    if (value === '') {
      const newFilters = { ...this.columnFilters };
      delete newFilters[colId];
      this.columnFilters = newFilters;
    } else {
      this.columnFilters = { ...this.columnFilters, [colId]: value };
    }
  }

  private handleHeaderClick(col: ColumnDef<T>): void {
    if (!col.sortable) return;

    if (this.sortColumnId === col.id) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumnId = col.id;
      this.sortDirection = 'asc';
    }
  }

  private getSortedDataFrom(data: T[]): T[] {
    if (!this.sortColumnId || !this.sortDirection) {
      return data;
    }

    const col = this.columns.find(c => c.id === this.sortColumnId);
    if (!col) return data;

    return [...data].sort((a, b) => {
      const aVal = col.accessor(a);
      const bVal = col.accessor(b);

      let comparison = 0;
      if (aVal === bVal) {
        comparison = 0;
      } else if (aVal == null) {
        comparison = 1;
      } else if (bVal == null) {
        comparison = -1;
      } else if (typeof aVal === 'string' && typeof bVal === 'string') {
        comparison = aVal.localeCompare(bVal);
      } else {
        comparison = aVal < bVal ? -1 : 1;
      }

      return this.sortDirection === 'desc' ? -comparison : comparison;
    });
  }

  private getSortIndicator(col: ColumnDef<T>): string {
    if (!col.sortable) return '';
    if (this.sortColumnId !== col.id) return '';
    return this.sortDirection === 'asc' ? '▲' : '▼';
  }

  private applyColumnFilters(data: T[]): T[] {
    const filterEntries = Object.entries(this.columnFilters);
    if (filterEntries.length === 0) {
      return data;
    }

    return data.filter(row =>
      filterEntries.every(([colId, filterValue]) => {
        const col = this.columns.find(c => c.id === colId);
        if (!col) return true;
        const value = col.accessor(row);
        return String(value) === filterValue;
      })
    );
  }

  private getFilteredData(): T[] {
    let result = this.data;

    // Apply column filters first
    result = this.applyColumnFilters(result);

    // Then apply global search
    if (!this.searchTerm.trim()) {
      return result;
    }

    const term = this.searchTerm.toLowerCase();
    const searchableColumns = this.columns.filter(col => col.searchable !== false);

    // If no columns are searchable, return current result
    if (searchableColumns.length === 0) {
      return result;
    }

    return result.filter(row =>
      searchableColumns.some(col => {
        const value = col.accessor(row);
        return value != null && String(value).toLowerCase().includes(term);
      })
    );
  }

  private renderTable(data: T[]) {
    return html`
      <table class="table table-zebra">
        <thead>
          <tr>
            ${this.columns.map(col => html`
              <th @click=${() => this.handleHeaderClick(col)}>
                ${col.header}
                ${col.sortable ? html`<span class="sort-indicator">${this.getSortIndicator(col)}</span>` : ''}
              </th>
            `)}
          </tr>
        </thead>
        <tbody>
          ${data.map(row => html`
            <tr>
              ${this.columns.map(col => html`<td>${col.accessor(row)}</td>`)}
            </tr>
          `)}
        </tbody>
      </table>
    `;
  }

  render() {
    if (this.data.length === 0 && this.emptyMessage) {
      return html`<div class="text-center p-4">${this.emptyMessage}</div>`;
    }

    const filteredData = this.getFilteredData();
    const sortedData = this.getSortedDataFrom(filteredData);

    const filterableColumns = this.columns.filter(col => col.filterable && col.filterType === 'select');

    return html`
      <input
        type="search"
        class="input input-bordered w-full mb-4"
        placeholder="Search..."
        @input=${this.handleSearchInput}
      />
      ${filterableColumns.length > 0 ? html`
        <div class="flex gap-2 mb-4">
          ${filterableColumns.map(col => html`
            <select
              class="select select-bordered"
              @change=${(e: Event) => this.handleColumnFilterChange(col.id, e)}
            >
              <option value="">All ${col.header}</option>
              ${col.filterOptions?.map(option => html`
                <option value="${option}">${option}</option>
              `)}
            </select>
          `)}
        </div>
      ` : ''}
      ${this.maxVisibleRows !== null
        ? html`
          <div class="table-scroll-container" style="max-height: calc(${this.maxVisibleRows} * 2.5rem + 3rem)">
            ${this.renderTable(sortedData)}
          </div>
        `
        : this.renderTable(sortedData)}
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'data-table': DataTable<unknown>;
  }
}
