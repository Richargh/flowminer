import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './author-radar-panel.ts';
import type { AuthorRadarPanel } from './author-radar-panel.ts';
import type { AuthorStats } from '../data-service.ts';

describe('AuthorRadarPanel', () => {
  let element: AuthorRadarPanel;

  const mockAuthors: AuthorStats[] = [
    { name: 'Alice', commitCount: 100, linesAdded: 5000, linesDeleted: 2000, avgCommitSize: 70 },
    { name: 'Bob', commitCount: 50, linesAdded: 2500, linesDeleted: 1000, avgCommitSize: 70 },
  ];

  beforeEach(async () => {
    element = document.createElement('author-radar-panel') as AuthorRadarPanel;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders both selector and radar chart', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;

    const selector = element.shadowRoot?.querySelector('author-selector');
    const chart = element.shadowRoot?.querySelector('author-radar-chart');

    expect(selector).toBeTruthy();
    expect(chart).toBeTruthy();
  });

  it('passes authors to selector', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;

    const selector = element.shadowRoot?.querySelector('author-selector') as HTMLElement & { authors: AuthorStats[] };
    expect(selector.authors).toEqual(mockAuthors);
  });

  it('updates radar chart when authors are selected', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    // Initially shows both authors (top 2 by default since < 5 authors)
    const chart = element.shadowRoot?.querySelector('author-radar-chart') as HTMLElement & { getChartOption: () => { series: { data: { name: string }[] }[] } | null };
    let chartOption = chart.getChartOption();
    // Authors sorted by commit count: Alice=100, Bob=50
    expect(chartOption?.series?.[0]?.data?.length).toBe(2);
    expect(chartOption?.series?.[0]?.data?.[0]?.name).toBe('Alice');
    expect(chartOption?.series?.[0]?.data?.[1]?.name).toBe('Bob');

    // Deselect Alice via dropdown
    const selector = element.shadowRoot?.querySelector('author-selector') as HTMLElement;
    const trigger = selector.shadowRoot?.querySelector('.dropdown-trigger') as HTMLElement;
    trigger.click();
    await element.updateComplete;

    const checkboxes = selector.shadowRoot?.querySelectorAll('input[type="checkbox"]') as NodeListOf<HTMLInputElement>;
    checkboxes[0].click();

    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    chartOption = chart.getChartOption();
    expect(chartOption?.series?.[0]?.data?.length).toBe(1);
    expect(chartOption?.series?.[0]?.data?.[0]?.name).toBe('Bob');
  });

  it('passes selected authors array to chart', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chart = element.shadowRoot?.querySelector('author-radar-chart') as HTMLElement & { authors: unknown[] };
    expect(Array.isArray(chart.authors)).toBe(true);
    expect(chart.authors.length).toBe(2);
  });
});
