import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import './ci-file-loader.ts';
import type { CiFileLoader } from './ci-file-loader.ts';

describe('CiFileLoader', () => {
  let element: CiFileLoader;

  beforeEach(async () => {
    element = document.createElement('ci-file-loader') as CiFileLoader;
    document.body.appendChild(element);
    await element.updateComplete;
  });

  afterEach(() => {
    element.remove();
  });

  it('renders a load button', () => {
    const button = element.shadowRoot?.querySelector('button');
    expect(button).toBeTruthy();
    expect(button?.textContent).toMatch(/load|ci/i);
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

  it('dispatches ci-data-loaded event when a JSONL file is loaded', async () => {
    const job1 = JSON.stringify({
      pipelineId: 1,
      pipelineRef: 'main',
      pipelineStatus: 'success',
      pipelineCreatedAt: '2024-01-01T10:00:00Z',
      jobId: 10,
      jobName: 'build',
      jobStage: 'build',
      jobStatus: 'success',
      jobStartedAt: '2024-01-01T10:01:00Z',
      jobFinishedAt: '2024-01-01T10:05:00Z',
      jobDurationSeconds: 240.0,
      jobAllowFailure: false
    });
    const job2 = JSON.stringify({
      pipelineId: 1,
      pipelineRef: 'main',
      pipelineStatus: 'success',
      pipelineCreatedAt: '2024-01-01T10:00:00Z',
      jobId: 11,
      jobName: 'test',
      jobStage: 'test',
      jobStatus: 'success',
      jobStartedAt: '2024-01-01T10:06:00Z',
      jobFinishedAt: '2024-01-01T10:10:00Z',
      jobDurationSeconds: 240.0,
      jobAllowFailure: false
    });
    const jsonlContent = `${job1}\n${job2}`;
    const file = new File([jsonlContent], 'pipelines.jsonl', { type: 'application/jsonl' });

    const eventPromise = new Promise<CustomEvent>((resolve) => {
      document.addEventListener('ci-data-loaded', ((e: CustomEvent) => resolve(e)) as EventListener, { once: true });
    });

    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    const input = element.shadowRoot?.querySelector('input[type="file"]') as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: dataTransfer.files });

    input.dispatchEvent(new Event('change'));

    const event = await eventPromise;
    expect(event.detail.jobs).toHaveLength(2);
  });
});
