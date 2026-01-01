import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './author-selector.ts';
import type { AuthorSelector } from './author-selector.ts';
import type { AuthorStats } from '../data-service.ts';

describe('AuthorSelector', () => {
  let element: AuthorSelector;

  const mockAuthors: AuthorStats[] = [
    { name: 'Alice', commitCount: 100, linesAdded: 5000, linesDeleted: 2000, avgCommitSize: 70 },
    { name: 'Bob', commitCount: 50, linesAdded: 2500, linesDeleted: 1000, avgCommitSize: 70 },
    { name: 'Charlie', commitCount: 75, linesAdded: 3500, linesDeleted: 1500, avgCommitSize: 67 },
  ];

  beforeEach(async () => {
    element = document.createElement('author-selector') as AuthorSelector;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders a dropdown trigger button', () => {
    const trigger = element.shadowRoot?.querySelector('.dropdown-trigger');
    expect(trigger).toBeTruthy();
  });

  it('displays authors in dropdown when opened', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;

    // Open dropdown
    const trigger = element.shadowRoot?.querySelector('.dropdown-trigger') as HTMLElement;
    trigger.click();
    await element.updateComplete;

    const items = element.shadowRoot?.querySelectorAll('.dropdown-item');
    expect(items).toHaveLength(3);
    // Authors are sorted by commit count: Alice=100, Charlie=75, Bob=50
    expect(items?.[0].textContent).toContain('Alice');
    expect(items?.[1].textContent).toContain('Charlie');
    expect(items?.[2].textContent).toContain('Bob');
  });

  it('emits author-selected event when checkbox changes', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;

    // Open dropdown
    const trigger = element.shadowRoot?.querySelector('.dropdown-trigger') as HTMLElement;
    trigger.click();
    await element.updateComplete;

    const eventPromise = new Promise<CustomEvent>((resolve) => {
      element.addEventListener('author-selected', (e) => resolve(e as CustomEvent));
    });

    // Click a checkbox
    const checkboxes = element.shadowRoot?.querySelectorAll('input[type="checkbox"]') as NodeListOf<HTMLInputElement>;
    checkboxes[1].click();

    const event = await eventPromise;
    expect(event.detail.author).toBeTruthy();
  });

  it('selects top authors by default', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;

    // With 3 authors, all should be selected
    expect(element.selectedAuthors.length).toBe(3);
  });

  it('exposes selectedAuthor property', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;

    expect(element.selectedAuthor?.name).toBe('Alice');
  });

  describe('multi-select dropdown', () => {
    it('opens dropdown menu when trigger is clicked', async () => {
      element.authors = mockAuthors;
      await element.updateComplete;

      // Verify dropdown is initially closed
      let menu = element.shadowRoot?.querySelector('.dropdown-menu');
      expect(menu).toBeFalsy();

      // Click trigger to open
      const trigger = element.shadowRoot?.querySelector('.dropdown-trigger') as HTMLElement;
      trigger.click();
      await element.updateComplete;

      // Verify dropdown is now open
      menu = element.shadowRoot?.querySelector('.dropdown-menu');
      expect(menu).toBeTruthy();
    });

    it('closes dropdown when clicking outside', async () => {
      element.authors = mockAuthors;
      await element.updateComplete;

      // Open dropdown
      const trigger = element.shadowRoot?.querySelector('.dropdown-trigger') as HTMLElement;
      trigger.click();
      await element.updateComplete;

      // Verify it's open
      let menu = element.shadowRoot?.querySelector('.dropdown-menu');
      expect(menu).toBeTruthy();

      // Click outside (on body)
      document.body.click();
      await element.updateComplete;

      // Verify it's closed
      menu = element.shadowRoot?.querySelector('.dropdown-menu');
      expect(menu).toBeFalsy();
    });

    it('renders dropdown with checkboxes when opened', async () => {
      element.authors = mockAuthors;
      await element.updateComplete;

      // Open dropdown
      const trigger = element.shadowRoot?.querySelector('.dropdown-trigger') as HTMLElement;
      trigger.click();
      await element.updateComplete;

      const checkboxes = element.shadowRoot?.querySelectorAll('input[type="checkbox"]');
      expect(checkboxes?.length).toBe(3);
    });

    it('emits authors-selected event with array when checkbox changes', async () => {
      element.authors = mockAuthors;
      await element.updateComplete;

      // Open dropdown
      const trigger = element.shadowRoot?.querySelector('.dropdown-trigger') as HTMLElement;
      trigger.click();
      await element.updateComplete;

      const eventPromise = new Promise<CustomEvent>((resolve) => {
        element.addEventListener('authors-selected', (e) => resolve(e as CustomEvent));
      });

      // Click a checkbox
      const checkboxes = element.shadowRoot?.querySelectorAll('input[type="checkbox"]') as NodeListOf<HTMLInputElement>;
      checkboxes[1].click();

      const event = await eventPromise;
      expect(Array.isArray(event.detail.authors)).toBe(true);
    });

    it('exposes selectedAuthors property returning array', async () => {
      element.authors = mockAuthors;
      await element.updateComplete;

      expect(Array.isArray(element.selectedAuthors)).toBe(true);
    });
  });

  describe('sorting by commit count', () => {
    it('sorts authors by commit count descending', async () => {
      // mockAuthors: Alice=100, Bob=50, Charlie=75
      // Expected order: Alice=100, Charlie=75, Bob=50
      element.authors = mockAuthors;
      await element.updateComplete;

      // Open dropdown
      const trigger = element.shadowRoot?.querySelector('.dropdown-trigger') as HTMLElement;
      trigger.click();
      await element.updateComplete;

      const items = element.shadowRoot?.querySelectorAll('.dropdown-item');
      expect(items?.[0].textContent).toContain('Alice');
      expect(items?.[1].textContent).toContain('Charlie');
      expect(items?.[2].textContent).toContain('Bob');
    });
  });

  describe('default selection', () => {
    it('selects top 5 authors by default', async () => {
      const manyAuthors: AuthorStats[] = [
        { name: 'A1', commitCount: 100, linesAdded: 1000, linesDeleted: 500, avgCommitSize: 10 },
        { name: 'A2', commitCount: 90, linesAdded: 900, linesDeleted: 450, avgCommitSize: 10 },
        { name: 'A3', commitCount: 80, linesAdded: 800, linesDeleted: 400, avgCommitSize: 10 },
        { name: 'A4', commitCount: 70, linesAdded: 700, linesDeleted: 350, avgCommitSize: 10 },
        { name: 'A5', commitCount: 60, linesAdded: 600, linesDeleted: 300, avgCommitSize: 10 },
        { name: 'A6', commitCount: 50, linesAdded: 500, linesDeleted: 250, avgCommitSize: 10 },
        { name: 'A7', commitCount: 40, linesAdded: 400, linesDeleted: 200, avgCommitSize: 10 },
      ];

      element.authors = manyAuthors;
      await element.updateComplete;

      expect(element.selectedAuthors.length).toBe(5);
      // Top 5 by commit count: A1, A2, A3, A4, A5
      const selectedNames = element.selectedAuthors.map(a => a.name);
      expect(selectedNames).toContain('A1');
      expect(selectedNames).toContain('A5');
      expect(selectedNames).not.toContain('A6');
    });

    it('selects all if fewer than 5 authors', async () => {
      element.authors = mockAuthors;
      await element.updateComplete;

      expect(element.selectedAuthors.length).toBe(3);
    });
  });

  describe('max 10 selection limit', () => {
    it('enforces max 10 selections', async () => {
      const manyAuthors: AuthorStats[] = Array.from({ length: 15 }, (_, i) => ({
        name: `Author${i + 1}`,
        commitCount: 100 - i,
        linesAdded: 1000,
        linesDeleted: 500,
        avgCommitSize: 10,
      }));

      element.authors = manyAuthors;
      await element.updateComplete;

      // Open dropdown
      const trigger = element.shadowRoot?.querySelector('.dropdown-trigger') as HTMLElement;
      trigger.click();
      await element.updateComplete;

      // Initially 5 selected, select 5 more to reach 10
      const checkboxes = element.shadowRoot?.querySelectorAll('input[type="checkbox"]') as NodeListOf<HTMLInputElement>;
      for (let i = 5; i < 10; i++) {
        checkboxes[i].click();
        await element.updateComplete;
      }

      expect(element.selectedAuthors.length).toBe(10);

      // Try to select 11th - should not change
      checkboxes[10].click();
      await element.updateComplete;

      expect(element.selectedAuthors.length).toBe(10);
    });
  });
});
