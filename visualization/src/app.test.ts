import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('App Shell', () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <author-radar-panel id="radar-panel"></author-radar-panel>
      <commit-timeline-panel id="timeline-panel"></commit-timeline-panel>
      <work-item-scatter-panel id="scatter-panel"></work-item-scatter-panel>
      <sankey-panel id="sankey-panel"></sankey-panel>
      <histogram-panel id="histogram-panel"></histogram-panel>
    `;
  });

  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('binds sample data to all chart panels', async () => {
    await import('./app');
    await new Promise(resolve => setTimeout(resolve, 100));

    const radarPanel = document.getElementById('radar-panel') as any;
    const timelinePanel = document.getElementById('timeline-panel') as any;
    const scatterPanel = document.getElementById('scatter-panel') as any;
    const sankeyPanel = document.getElementById('sankey-panel') as any;
    const histogramPanel = document.getElementById('histogram-panel') as any;

    expect(radarPanel.authors).toBeDefined();
    expect(radarPanel.authors.length).toBeGreaterThan(0);

    expect(timelinePanel.timeline).toBeDefined();
    expect(timelinePanel.timeline.length).toBeGreaterThan(0);

    expect(scatterPanel.workItems).toBeDefined();
    expect(scatterPanel.workItems.length).toBeGreaterThan(0);

    expect(sankeyPanel.flow).toBeDefined();
    expect(sankeyPanel.flow.nodes.length).toBeGreaterThan(0);

    expect(histogramPanel.buckets).toBeDefined();
    expect(histogramPanel.buckets.length).toBeGreaterThan(0);
  });
});
