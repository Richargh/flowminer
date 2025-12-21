import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './author-radar-chart';
import type { AuthorRadarChart } from './author-radar-chart';
import type { AuthorStats } from '../data-service';

describe('AuthorRadarChart', () => {
  let element: AuthorRadarChart;

  beforeEach(async () => {
    element = document.createElement('author-radar-chart') as AuthorRadarChart;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders a container element for the radar chart', () => {
    const container = element.shadowRoot?.querySelector('.chart-container');
    expect(container).toBeTruthy();
    expect(container?.tagName.toLowerCase()).toBe('div');
  });

  it('initializes ECharts instance on the container', async () => {
    // Wait for chart initialization
    await new Promise(resolve => setTimeout(resolve, 100));

    const container = element.shadowRoot?.querySelector('.chart-container') as HTMLElement;
    // ECharts adds a canvas element when initialized
    const canvas = container?.querySelector('canvas');
    expect(canvas).toBeTruthy();
  });

  it('has default dimensions for the chart container', () => {
    const container = element.shadowRoot?.querySelector('.chart-container') as HTMLElement;
    const computedStyle = window.getComputedStyle(container);
    // Height should be 400px as defined in CSS
    expect(computedStyle.height).toBe('400px');
  });

  it('displays author data when author property is set', async () => {
    const sampleAuthor: AuthorStats = {
      name: 'Alice',
      commitCount: 100,
      linesAdded: 5000,
      linesDeleted: 2000,
      avgCommitSize: 70,
    };

    element.author = sampleAuthor;
    await element.updateComplete;
    // Wait for chart to update
    await new Promise(resolve => setTimeout(resolve, 100));

    // The chart should have been updated with the author data
    // We verify by checking the chart option contains the author name
    const chartOption = element.getChartOption();
    expect(chartOption).toBeTruthy();
    expect(chartOption?.series?.[0]?.data?.[0]?.name).toBe('Alice');
  });

  it('updates chart when author data changes', async () => {
    const alice: AuthorStats = {
      name: 'Alice',
      commitCount: 100,
      linesAdded: 5000,
      linesDeleted: 2000,
      avgCommitSize: 70,
    };
    const bob: AuthorStats = {
      name: 'Bob',
      commitCount: 50,
      linesAdded: 2500,
      linesDeleted: 1000,
      avgCommitSize: 70,
    };

    element.author = alice;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    element.author = bob;
    await element.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 100));

    const chartOption = element.getChartOption();
    expect(chartOption?.series?.[0]?.data?.[0]?.name).toBe('Bob');
    expect(chartOption?.series?.[0]?.data?.[0]?.value?.[0]).toBe(50);
  });
});
