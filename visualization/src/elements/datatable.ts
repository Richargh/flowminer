import { LitElement, html } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

export interface ColumnDef<T> {
  id: string;
  header: string;
  accessor: (row: T) => unknown;
  sortable?: boolean;
  searchable?: boolean;
  filterable?: boolean;
  filterType?: 'select' | 'multiSelect';
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

  @state()
  private multiSelectFilters: Record<string, Set<string>> = {};

  // Use light DOM so DaisyUI global styles apply
  createRenderRoot() {
    return this;
  }

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

  private handleMultiSelectChange(colId: string, option: string, checked: boolean): void {
    const currentSet = this.multiSelectFilters[colId] ?? new Set<string>();
    const newSet = new Set(currentSet);

    if (checked) {
      newSet.add(option);
    } else {
      newSet.delete(option);
    }

    this.multiSelectFilters = { ...this.multiSelectFilters, [colId]: newSet };
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
    const multiSelectEntries = Object.entries(this.multiSelectFilters);

    if (filterEntries.length === 0 && multiSelectEntries.length === 0) {
      return data;
    }

    return data.filter(row => {
      // Apply single-select filters
      const passesSingleSelect = filterEntries.every(([colId, filterValue]) => {
        const col = this.columns.find(c => c.id === colId);
        if (!col) return true;
        const value = col.accessor(row);
        return String(value) === filterValue;
      });

      // Apply multi-select filters (only if any options are selected)
      const passesMultiSelect = multiSelectEntries.every(([colId, selectedValues]) => {
        if (selectedValues.size === 0) return true; // No filter applied
        const col = this.columns.find(c => c.id === colId);
        if (!col) return true;
        const value = col.accessor(row);
        return selectedValues.has(String(value));
      });

      return passesSingleSelect && passesMultiSelect;
    });
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
              <th class="${col.sortable ? 'cursor-pointer' : ''}" @click=${() => this.handleHeaderClick(col)}>
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

    const filterableColumns = this.columns.filter(col => col.filterable && (col.filterType === 'select' || col.filterType === 'multiSelect'));

    return html`
      <input
        type="search"
        class="input input-bordered w-full mb-4"
        placeholder="Search..."
        @input=${this.handleSearchInput}
      />
      ${filterableColumns.length > 0 ? html`
        <div class="flex gap-2 mb-4">
          ${filterableColumns.map(col => {
            if (col.filterType === 'multiSelect') {
              const selectedCount = this.multiSelectFilters[col.id]?.size ?? 0;
              const buttonLabel = selectedCount > 0
                ? `${col.header} (${selectedCount})`
                : `All ${col.header}`;
              return html`
                <div class="dropdown">
                  <div tabindex="0" role="button" class="select select-bordered flex items-center">${buttonLabel}</div>
                  <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-10 w-52 p-2 shadow-lg">
                    ${col.filterOptions?.map(option => html`
                      <li>
                        <label class="flex items-center gap-2 cursor-pointer">
                          <input
                            type="checkbox"
                            class="checkbox checkbox-sm"
                            .checked=${this.multiSelectFilters[col.id]?.has(option) ?? false}
                            @change=${(e: Event) => this.handleMultiSelectChange(col.id, option, (e.target as HTMLInputElement).checked)}
                          />
                          <span>${option}</span>
                        </label>
                      </li>
                    `)}
                  </ul>
                </div>
              `;
            } else {
              return html`
                <select
                  class="select select-bordered"
                  @change=${(e: Event) => this.handleColumnFilterChange(col.id, e)}
                >
                  <option value="">All ${col.header}</option>
                  ${col.filterOptions?.map(option => html`
                    <option value="${option}">${option}</option>
                  `)}
                </select>
              `;
            }
          })}
        </div>
      ` : ''}
      ${this.maxVisibleRows !== null
        ? html`
          <div class="overflow-y-auto" style="max-height: calc(${this.maxVisibleRows} * 2.5rem + 3rem)">
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
