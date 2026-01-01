import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './theme-switcher';
import type { ThemeSwitcher } from './theme-switcher';

describe('ThemeSwitcher', () => {
  let element: ThemeSwitcher;

  beforeEach(async () => {
    // Set initial theme
    document.documentElement.setAttribute('data-theme', 'light');
    element = document.createElement('theme-switcher') as ThemeSwitcher;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
    document.documentElement.removeAttribute('data-theme');
  });

  it('renders a toggle switch', () => {
    const toggle = element.shadowRoot?.querySelector('input[type="checkbox"]');
    expect(toggle).toBeTruthy();
  });

  it('displays theme labels', () => {
    const textContent = element.shadowRoot?.textContent ?? '';
    expect(textContent).toMatch(/light|dark/i);
  });

  it('starts with light theme by default', () => {
    const toggle = element.shadowRoot?.querySelector('input[type="checkbox"]') as HTMLInputElement;
    expect(toggle?.checked).toBe(false);
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('switches to dark theme when toggle is clicked', async () => {
    const toggle = element.shadowRoot?.querySelector('input[type="checkbox"]') as HTMLInputElement;

    toggle.click();
    await element.updateComplete;

    expect(toggle.checked).toBe(true);
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('switches back to light theme on second click', async () => {
    const toggle = element.shadowRoot?.querySelector('input[type="checkbox"]') as HTMLInputElement;

    toggle.click();
    await element.updateComplete;
    toggle.click();
    await element.updateComplete;

    expect(toggle.checked).toBe(false);
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });
});
