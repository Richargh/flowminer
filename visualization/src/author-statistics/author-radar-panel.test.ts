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

  it('updates radar chart when author is selected', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    // Initially shows first author
    const chart = element.shadowRoot?.querySelector('author-radar-chart') as HTMLElement & { getChartOption: () => { series: { data: { name: string }[] }[] } | null };
    let chartOption = chart.getChartOption();
    expect(chartOption?.series?.[0]?.data?.[0]?.name).toBe('Alice');

    // Simulate selecting Bob
    const selector = element.shadowRoot?.querySelector('author-selector') as HTMLElement;
    const select = selector.shadowRoot?.querySelector('select') as HTMLSelectElement;
    select.value = 'Bob';
    select.dispatchEvent(new Event('change'));

    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    chartOption = chart.getChartOption();
    expect(chartOption?.series?.[0]?.data?.[0]?.name).toBe('Bob');
  });
});
