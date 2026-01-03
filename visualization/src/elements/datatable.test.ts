import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './datatable.ts';
import type { DataTable, ColumnDef } from './datatable.ts';

interface Person {
  name: string;
  age: number;
}

describe('DataTable', () => {
  let element: DataTable<Person>;

  beforeEach(async () => {
    element = document.createElement('data-table') as DataTable<Person>;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders without error when created', () => {
    expect(element).toBeTruthy();
    expect(element.shadowRoot).toBeTruthy();
  });

  it('accepts data and columns properties', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name }
    ];

    element.data = [{ name: 'Alice', age: 30 }];
    element.columns = columns;
    await element.updateComplete;

    expect(element.data).toHaveLength(1);
    expect(element.columns).toHaveLength(1);
  });

  it('renders table with headers and data rows', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name }
    ];

    element.data = [{ name: 'Alice', age: 30 }];
    element.columns = columns;
    await element.updateComplete;

    const header = element.shadowRoot?.querySelector('th');
    expect(header?.textContent).toContain('Name');

    const cell = element.shadowRoot?.querySelector('td');
    expect(cell?.textContent).toContain('Alice');
  });

  it('shows empty message when data is empty', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name }
    ];

    element.data = [];
    element.columns = columns;
    element.emptyMessage = 'No data available';
    await element.updateComplete;

    const emptyText = element.shadowRoot?.textContent;
    expect(emptyText).toContain('No data available');
  });

  it('has DaisyUI table classes', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name }
    ];

    element.data = [{ name: 'Alice', age: 30 }];
    element.columns = columns;
    await element.updateComplete;

    const table = element.shadowRoot?.querySelector('table');
    expect(table?.classList.contains('table')).toBe(true);
    expect(table?.classList.contains('table-zebra')).toBe(true);
  });

  it('shows sort indicator on sortable column header', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name, sortable: true }
    ];

    element.data = [{ name: 'Alice', age: 30 }];
    element.columns = columns;
    await element.updateComplete;

    const header = element.shadowRoot?.querySelector('th');
    const sortIndicator = header?.querySelector('.sort-indicator');
    expect(sortIndicator).toBeTruthy();
  });

  it('sorts data ascending when sortable header is clicked', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name, sortable: true }
    ];

    element.data = [{ name: 'Bob', age: 25 }, { name: 'Alice', age: 30 }];
    element.columns = columns;
    await element.updateComplete;

    const header = element.shadowRoot?.querySelector('th');
    header?.click();
    await element.updateComplete;

    const cells = element.shadowRoot?.querySelectorAll('td');
    expect(cells?.[0]?.textContent).toContain('Alice');
    expect(cells?.[1]?.textContent).toContain('Bob');
  });

  it('sorts data descending when sortable header is clicked twice', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name, sortable: true }
    ];

    element.data = [{ name: 'Alice', age: 30 }, { name: 'Bob', age: 25 }];
    element.columns = columns;
    await element.updateComplete;

    const header = element.shadowRoot?.querySelector('th');
    header?.click(); // First click - ascending
    await element.updateComplete;
    header?.click(); // Second click - descending
    await element.updateComplete;

    const cells = element.shadowRoot?.querySelectorAll('td');
    expect(cells?.[0]?.textContent).toContain('Bob');
    expect(cells?.[1]?.textContent).toContain('Alice');
  });

  it('does not sort when non-sortable header is clicked', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name, sortable: false }
    ];

    element.data = [{ name: 'Bob', age: 25 }, { name: 'Alice', age: 30 }];
    element.columns = columns;
    await element.updateComplete;

    const header = element.shadowRoot?.querySelector('th');
    header?.click();
    await element.updateComplete;

    const cells = element.shadowRoot?.querySelectorAll('td');
    expect(cells?.[0]?.textContent).toContain('Bob');
    expect(cells?.[1]?.textContent).toContain('Alice');
  });

  it('renders search input above table', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name }
    ];

    element.data = [{ name: 'Alice', age: 30 }];
    element.columns = columns;
    await element.updateComplete;

    const searchInput = element.shadowRoot?.querySelector('input[type="search"]');
    expect(searchInput).toBeTruthy();
  });

  it('filters rows based on search term', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name }
    ];

    element.data = [{ name: 'Alice', age: 30 }, { name: 'Bob', age: 25 }];
    element.columns = columns;
    await element.updateComplete;

    const searchInput = element.shadowRoot?.querySelector('input[type="search"]') as HTMLInputElement;
    searchInput.value = 'Alice';
    searchInput.dispatchEvent(new Event('input'));
    await element.updateComplete;

    const rows = element.shadowRoot?.querySelectorAll('tbody tr');
    expect(rows).toHaveLength(1);
    expect(rows?.[0]?.textContent).toContain('Alice');
  });

  it('only searches in searchable columns', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name, searchable: true },
      { id: 'age', header: 'Age', accessor: (row) => row.age, searchable: false }
    ];

    element.data = [{ name: 'Alice', age: 30 }, { name: 'Bob', age: 25 }];
    element.columns = columns;
    await element.updateComplete;

    const searchInput = element.shadowRoot?.querySelector('input[type="search"]') as HTMLInputElement;
    searchInput.value = '30';  // Searching for age, which is not searchable
    searchInput.dispatchEvent(new Event('input'));
    await element.updateComplete;

    const rows = element.shadowRoot?.querySelectorAll('tbody tr');
    expect(rows).toHaveLength(0);
  });

  it('renders select filter for filterable column', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name, filterable: true, filterType: 'select', filterOptions: ['Alice', 'Bob'] }
    ];

    element.data = [{ name: 'Alice', age: 30 }, { name: 'Bob', age: 25 }];
    element.columns = columns;
    await element.updateComplete;

    const select = element.shadowRoot?.querySelector('select');
    expect(select).toBeTruthy();
    expect(select?.querySelectorAll('option')).toHaveLength(3); // "All" + 2 options
  });

  it('filters rows when column filter is changed', async () => {
    const columns: ColumnDef<Person>[] = [
      { id: 'name', header: 'Name', accessor: (row) => row.name, filterable: true, filterType: 'select', filterOptions: ['Alice', 'Bob'] }
    ];

    element.data = [{ name: 'Alice', age: 30 }, { name: 'Bob', age: 25 }];
    element.columns = columns;
    await element.updateComplete;

    const select = element.shadowRoot?.querySelector('select') as HTMLSelectElement;
    select.value = 'Alice';
    select.dispatchEvent(new Event('change'));
    await element.updateComplete;

    const rows = element.shadowRoot?.querySelectorAll('tbody tr');
    expect(rows).toHaveLength(1);
    expect(rows?.[0]?.textContent).toContain('Alice');
  });
});
