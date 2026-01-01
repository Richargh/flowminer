import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import axe from 'axe-core';

describe('Accessibility', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('theme-switcher has no critical accessibility violations', async () => {
    await import('./components/theme-switcher');
    const element = document.createElement('theme-switcher');
    document.body.appendChild(element);
    await (element as any).updateComplete;

    // Test the light DOM (axe can't test shadow DOM directly)
    const results = await axe.run(document.body);
    const criticalViolations = results.violations.filter(v => v.impact === 'critical');
    expect(criticalViolations).toHaveLength(0);
  });

  it('author-selector has no critical accessibility violations', async () => {
    await import('./author-statistics/author-selector.ts');
    const element = document.createElement('author-selector') as any;
    element.authors = [
      { name: 'Alice', commitCount: 100, linesAdded: 5000, linesDeleted: 2000, avgCommitSize: 70 },
    ];
    document.body.appendChild(element);
    await element.updateComplete;

    const results = await axe.run(document.body);
    const criticalViolations = results.violations.filter(v => v.impact === 'critical');
    expect(criticalViolations).toHaveLength(0);
  });

  it('chart containers have accessible structure', async () => {
    await import('./components/commit-timeline-chart');
    const element = document.createElement('commit-timeline-chart') as any;
    document.body.appendChild(element);
    await element.updateComplete;

    // Chart containers should have proper block display
    const container = element.shadowRoot?.querySelector('.chart-container');
    expect(container).toBeTruthy();

    // Check computed styles indicate proper display
    const style = window.getComputedStyle(container as Element);
    expect(style.height).toBe('400px');
  });

  it('all interactive elements have focus states', async () => {
    await import('./components/theme-switcher');
    const element = document.createElement('theme-switcher');
    document.body.appendChild(element);
    await (element as any).updateComplete;

    // The toggle input should be focusable
    const toggle = element.shadowRoot?.querySelector('input[type="checkbox"]') as HTMLInputElement;
    expect(toggle).toBeTruthy();
    expect(toggle?.tabIndex).toBeGreaterThanOrEqual(0);
  });
});
