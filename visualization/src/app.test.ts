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
});
