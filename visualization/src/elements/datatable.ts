import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export interface ColumnDef<T> {
  id: string;
  header: string;
  accessor: (row: T) => unknown;
}

@customElement('data-table')
export class DataTable<T> extends LitElement {
  @property({ type: Array })
  data: T[] = [];

  @property({ type: Array })
  columns: ColumnDef<T>[] = [];

  static styles = css`
    :host {
      display: block;
    }
  `;

  render() {
    return html`
      <table class="table table-zebra">
        <thead>
          <tr>
            ${this.columns.map(col => html`<th>${col.header}</th>`)}
          </tr>
        </thead>
        <tbody>
          ${this.data.map(row => html`
            <tr>
              ${this.columns.map(col => html`<td>${col.accessor(row)}</td>`)}
            </tr>
          `)}
        </tbody>
      </table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'data-table': DataTable<unknown>;
  }
}
