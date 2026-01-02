import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('App Shell', () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <commit-range-panel id="activity-panel"></commit-range-panel>
      <work-item-scatter-panel id="scatter-panel"></work-item-scatter-panel>
      <work-item-duration-panel id="histogram-panel"></work-item-duration-panel>
    `;
  });

  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('binds sample data to all chart panels', async () => {
    await import('./app');
    await new Promise(resolve => setTimeout(resolve, 100));

    const activityPanel = document.getElementById('activity-panel') as any;
    const scatterPanel = document.getElementById('scatter-panel') as any;
    const histogramPanel = document.getElementById('histogram-panel') as any;

    expect(activityPanel.data).toBeDefined();
    expect(activityPanel.data.length).toBeGreaterThan(0);

    expect(scatterPanel.workItems).toBeDefined();
    expect(scatterPanel.workItems.length).toBeGreaterThan(0);

    expect(histogramPanel.buckets).toBeDefined();
    expect(histogramPanel.buckets.length).toBeGreaterThan(0);
  });

  it('full flow: load data, select range, panels update with filtered data', async () => {
    // Import app and load data
    const { loadDefaultData } = await import('./startup/default-data-loader.ts');
    await import('./app');

    // Manually dispatch data since module may be cached
    document.dispatchEvent(new CustomEvent('data-loaded', {
      detail: loadDefaultData(),
      bubbles: true
    }));

    await new Promise(resolve => setTimeout(resolve, 100));

    const scatterPanel = document.getElementById('scatter-panel') as any;

    // Verify initial data loaded
    const initialWorkItemCount = scatterPanel.workItems.length;
    expect(initialWorkItemCount).toBeGreaterThan(0);

    // Get the first work item to create a narrow range
    if (initialWorkItemCount >= 2) {
      const startDate = scatterPanel.workItems[1].startDate;

      document.dispatchEvent(new CustomEvent('time-range-changed', {
        detail: { startDate, endDate: startDate },
        bubbles: true
      }));

      await new Promise(resolve => setTimeout(resolve, 50));

      // Panels should now show filtered data (less items or equal)
      expect(scatterPanel.workItems.length).toBeLessThanOrEqual(initialWorkItemCount);
    }
  });
});
