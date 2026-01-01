import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import './file-loader';
import type { FileLoader } from './file-loader';

import type {GitMiningResult} from "../commit-mining/app/api-types/git-mining-result.ts";

describe('FileLoader', () => {
  let element: FileLoader;

  beforeEach(async () => {
    element = document.createElement('file-loader') as FileLoader;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders a load button', () => {
    const button = element.shadowRoot?.querySelector('button');
    expect(button).toBeTruthy();
    expect(button?.textContent).toMatch(/load|open/i);
  });

  it('renders a hidden file input', () => {
    const input = element.shadowRoot?.querySelector('input[type="file"]') as HTMLInputElement;
    expect(input).toBeTruthy();
    expect(input?.accept).toBe('.jsonl');
  });

  it('clicking button triggers file input click', async () => {
    const input = element.shadowRoot?.querySelector('input[type="file"]') as HTMLInputElement;
    const clickSpy = vi.spyOn(input, 'click');

    const button = element.shadowRoot?.querySelector('button') as HTMLButtonElement;
    button.click();

    expect(clickSpy).toHaveBeenCalled();
  });

  it('dispatches data-loaded event with mined data when file is loaded', async () => {
    const jsonlContent = '{"hash":"abc123","authorName":"Alice","authorEmail":"alice@example.com","date":"2024-01-01T10:00:00Z","message":"Test commit","parents":[],"branchId":{"type":"certain","name":"main","tipCommit":null},"workKeys":[],"commitType":"UNKNOWN","fileChanges":[{"path":"src/main.ts","additions":10,"deletions":5,"isRename":false,"oldPath":null}],"isOnCurrentBranch":true,"isMerge":false}';
    const file = new File([jsonlContent], 'test.jsonl', { type: 'application/jsonl' });

    const eventPromise = new Promise<CustomEvent<GitMiningResult>>((resolve) => {
      element.addEventListener('data-loaded', ((e: CustomEvent<GitMiningResult>) => resolve(e)) as EventListener);
    });

    const input = element.shadowRoot?.querySelector('input[type="file"]') as HTMLInputElement;
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    input.files = dataTransfer.files;
    input.dispatchEvent(new Event('change'));

    const event = await eventPromise;
    const authors = event.detail.authorStatistics.all();
    expect(authors.length).toBeGreaterThan(0);
    expect(authors[0].author.name).toBe('Alice');
  });

  it('shows filename after loading', async () => {
    const jsonlContent = '{"hash":"abc123","authorName":"Alice","authorEmail":"alice@example.com","date":"2024-01-01T10:00:00Z","message":"Test","parents":[],"branchId":{"type":"certain","name":"main","tipCommit":null},"workKeys":[],"commitType":"UNKNOWN","fileChanges":[],"isOnCurrentBranch":true,"isMerge":false}';
    const file = new File([jsonlContent], 'mydata.jsonl', { type: 'application/jsonl' });

    const input = element.shadowRoot?.querySelector('input[type="file"]') as HTMLInputElement;
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    input.files = dataTransfer.files;
    input.dispatchEvent(new Event('change'));

    // Wait for file read and update
    await new Promise(resolve => setTimeout(resolve, 100));
    await element.updateComplete;

    const text = element.shadowRoot?.textContent ?? '';
    expect(text).toContain('mydata.jsonl');
  });
});
