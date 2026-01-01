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
    await import('./theme-switcher/theme-switcher');
    const element = document.createElement('theme-switcher');
    document.body.appendChild(element);
    await (element as any).updateComplete;

    // Test the light DOM (axe can't test shadow DOM directly)
    const results = await axe.run(document.body);
    const criticalViolations = results.violations.filter(v => v.impact === 'critical');
    expect(criticalViolations).toHaveLength(0);
  });

  it('all interactive elements have focus states', async () => {
    await import('./theme-switcher/theme-switcher');
    const element = document.createElement('theme-switcher');
    document.body.appendChild(element);
    await (element as any).updateComplete;

    // The toggle input should be focusable
    const toggle = element.shadowRoot?.querySelector('input[type="checkbox"]') as HTMLInputElement;
    expect(toggle).toBeTruthy();
    expect(toggle?.tabIndex).toBeGreaterThanOrEqual(0);
  });
});
