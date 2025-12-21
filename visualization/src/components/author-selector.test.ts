import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import './author-selector';
import type { AuthorSelector } from './author-selector';
import type { AuthorStats } from '../data-service';

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

  it('renders a select element', () => {
    const select = element.shadowRoot?.querySelector('select');
    expect(select).toBeTruthy();
  });

  it('displays authors as options when authors property is set', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;

    const options = element.shadowRoot?.querySelectorAll('option');
    expect(options).toHaveLength(3);
    expect(options?.[0].textContent?.trim()).toBe('Alice');
    expect(options?.[1].textContent?.trim()).toBe('Bob');
    expect(options?.[2].textContent?.trim()).toBe('Charlie');
  });

  it('emits author-selected event when selection changes', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;

    const eventPromise = new Promise<CustomEvent>((resolve) => {
      element.addEventListener('author-selected', (e) => resolve(e as CustomEvent));
    });

    const select = element.shadowRoot?.querySelector('select') as HTMLSelectElement;
    select.value = 'Bob';
    select.dispatchEvent(new Event('change'));

    const event = await eventPromise;
    expect(event.detail.author.name).toBe('Bob');
  });

  it('selects first author by default', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;

    const select = element.shadowRoot?.querySelector('select') as HTMLSelectElement;
    expect(select.value).toBe('Alice');
  });

  it('exposes selectedAuthor property', async () => {
    element.authors = mockAuthors;
    await element.updateComplete;

    expect(element.selectedAuthor?.name).toBe('Alice');
  });
});
