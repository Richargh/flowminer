import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('App Shell', () => {
  beforeEach(() => {
    document.body.innerHTML = '<div id="app"></div>';
  });

  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('renders all five chart panels in a responsive grid layout with DaisyUI cards', async () => {
    // Import app to trigger rendering
    await import('./app');

    // Wait for custom elements to be defined
    await new Promise(resolve => setTimeout(resolve, 100));

    const app = document.getElementById('app');
    expect(app).toBeTruthy();

    // Check for all 5 chart panels
    expect(app?.querySelector('author-radar-panel')).toBeTruthy();
    expect(app?.querySelector('commit-timeline-panel')).toBeTruthy();
    expect(app?.querySelector('work-item-scatter-panel')).toBeTruthy();
    expect(app?.querySelector('sankey-panel')).toBeTruthy();
    expect(app?.querySelector('histogram-panel')).toBeTruthy();

    // Check for grid layout and DaisyUI classes in HTML
    const html = app?.innerHTML ?? '';
    expect(html).toContain('grid');
    expect(html).toContain('card');
  });
});
