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
});
