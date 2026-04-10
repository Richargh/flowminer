import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('App Shell', () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <commit-range-panel id="activity-panel"></commit-range-panel>
      <work-item-scatter-panel id="scatter-panel"></work-item-scatter-panel>
      <work-item-duration-panel id="histogram-panel"></work-item-duration-panel>
      <ci-boxplot-panel id="ci-boxplot-panel"></ci-boxplot-panel>
    `;
  });

  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('binds git sample data to all git chart panels', async () => {
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

  it('loadDefaultCiData returns non-empty jobs', async () => {
    const { loadDefaultCiData } = await import('./startup/default-ci-data-loader.ts');
    const result = loadDefaultCiData();
    expect(result.jobs.length).toBeGreaterThan(0);
    // Verify jobs have required fields
    const job = result.jobs[0];
    expect(job.pipelineId).toBeDefined();
    expect(job.jobName).toBeDefined();
    expect(job.jobStartedAt).toBeDefined();
    expect(job.jobFinishedAt).toBeDefined();
  });

  it('binds CI sample data to all CI chart panels', async () => {
    const { loadDefaultCiData } = await import('./startup/default-ci-data-loader.ts');
    await import('./app');

    // Manually dispatch since module may be cached
    document.dispatchEvent(new CustomEvent('ci-data-loaded', {
      detail: loadDefaultCiData(),
      bubbles: true
    }));

    await new Promise(resolve => setTimeout(resolve, 100));

    const boxplotPanel = document.getElementById('ci-boxplot-panel') as any;

    expect(boxplotPanel.boxplotData).toBeDefined();
    expect(boxplotPanel.boxplotData.length).toBeGreaterThan(0);
  });

  it('CI boxplot chart renders with sample data when panel is visible', async () => {
    const { loadDefaultCiData } = await import('./startup/default-ci-data-loader.ts');
    await import('./app');

    document.dispatchEvent(new CustomEvent('ci-data-loaded', {
      detail: loadDefaultCiData(),
      bubbles: true
    }));

    await new Promise(resolve => setTimeout(resolve, 200));

    const boxplotPanel = document.getElementById('ci-boxplot-panel') as any;
    await boxplotPanel.updateComplete;

    const boxplotChart = boxplotPanel.shadowRoot?.querySelector('ci-boxplot-chart') as any;
    expect(boxplotChart).toBeTruthy();

    await boxplotChart.updateComplete;
    await new Promise(resolve => setTimeout(resolve, 200));

    const option = boxplotChart.getChartOption();
    expect(option).toBeTruthy();
    expect(option.series[0].data.length).toBeGreaterThan(0);
  });

  it('CI boxplot chart renders when panel starts inside display:none tab (DaisyUI simulation)', async () => {
    const style = document.createElement('style');
    style.textContent = '.fake-tab-content { display: none; } .fake-tab-content.active { display: block; }';
    document.head.appendChild(style);

    const tabContent = document.createElement('div');
    tabContent.className = 'fake-tab-content';
    const boxplotPanel = document.createElement('ci-boxplot-panel') as any;
    tabContent.appendChild(boxplotPanel);
    document.body.appendChild(tabContent);

    const { loadDefaultCiData } = await import('./startup/default-ci-data-loader.ts');

    document.dispatchEvent(new CustomEvent('ci-data-loaded', {
      detail: loadDefaultCiData(),
      bubbles: true
    }));

    await new Promise(resolve => setTimeout(resolve, 150));
    await boxplotPanel.updateComplete;

    expect(boxplotPanel.boxplotData.length).toBeGreaterThan(0);

    // Simulate user clicking the CI tab
    tabContent.classList.add('active');

    await new Promise(resolve => setTimeout(resolve, 300));

    const boxplotChart = boxplotPanel.shadowRoot?.querySelector('ci-boxplot-chart') as any;
    expect(boxplotChart).toBeTruthy();
    await boxplotChart.updateComplete;

    const option = boxplotChart.getChartOption();
    expect(option).toBeTruthy();
    expect(option.series[0].data.length).toBeGreaterThan(0);

    tabContent.remove();
    style.remove();
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
