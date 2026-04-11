import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import type { PipelineJob } from './ci-types.ts';
import type { CiMiningResult } from './ci-file-loader.ts';
import type { GanttEntry } from './ci-gantt-chart.ts';
import './ci-gantt-chart.ts';

interface PipelineRun {
  readonly pipelineId: number;
  readonly pipelineRef: string;
  readonly pipelineCreatedAt: string;
  readonly pipelineStatus: string;
}

function extractPipelineRuns(jobs: PipelineJob[]): PipelineRun[] {
  const seen = new Map<number, PipelineRun>();
  for (const job of jobs) {
    if (!seen.has(job.pipelineId)) {
      seen.set(job.pipelineId, {
        pipelineId: job.pipelineId,
        pipelineRef: job.pipelineRef,
        pipelineCreatedAt: job.pipelineCreatedAt,
        pipelineStatus: job.pipelineStatus,
      });
    }
  }
  return [...seen.values()].sort((a, b) =>
    new Date(b.pipelineCreatedAt).getTime() - new Date(a.pipelineCreatedAt).getTime()
  );
}

function jobsToGanttEntries(jobs: PipelineJob[], pipelineId: number): GanttEntry[] {
  const pipelineJobs = jobs.filter(j => j.pipelineId === pipelineId);
  const pipelineStart = pipelineJobs[0]?.pipelineCreatedAt;
  if (!pipelineStart) return [];

  const startMs = new Date(pipelineStart).getTime();
  return pipelineJobs
    .filter(j => j.jobStartedAt != null && j.jobDurationSeconds != null)
    .map(j => ({
      jobName: j.jobName,
      jobStatus: j.jobStatus,
      pipelineId: j.pipelineId,
      offsetSeconds: (new Date(j.jobStartedAt!).getTime() - startMs) / 1000,
      durationSeconds: j.jobDurationSeconds!,
    }));
}

function formatPipelineLabel(run: PipelineRun): string {
  const date = new Date(run.pipelineCreatedAt).toLocaleDateString();
  return `#${run.pipelineId} — ${run.pipelineRef} (${date}) [${run.pipelineStatus}]`;
}

@customElement('ci-gantt-panel')
export class CiGanttPanel extends LitElement {
  @state()
  private allJobs: PipelineJob[] = [];

  @state()
  private pipelineRuns: PipelineRun[] = [];

  @state()
  private selectedPipelineId: number | null = null;

  private handleCiDataLoaded = (event: Event): void => {
    const customEvent = event as CustomEvent<CiMiningResult>;
    this.allJobs = customEvent.detail.jobs;
    this.pipelineRuns = extractPipelineRuns(this.allJobs);
    if (this.pipelineRuns.length > 0) {
      this.selectedPipelineId = this.pipelineRuns[0].pipelineId;
    }
  };

  connectedCallback(): void {
    super.connectedCallback();
    document.addEventListener('ci-data-loaded', this.handleCiDataLoaded);
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    document.removeEventListener('ci-data-loaded', this.handleCiDataLoaded);
  }

  static styles = css`
    :host { display: block; }
    .controls { margin-bottom: 1rem; }
    .empty-state {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 300px;
      color: var(--text-secondary, #666);
      font-style: italic;
    }
  `;

  private onPipelineChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedPipelineId = Number(select.value);
  }

  render() {
    if (this.pipelineRuns.length === 0) {
      return html`<div class="empty-state">No CI pipeline data loaded</div>`;
    }

    const ganttData = this.selectedPipelineId != null
      ? jobsToGanttEntries(this.allJobs, this.selectedPipelineId)
      : [];

    return html`
      <div class="controls">
        <select class="select select-bordered w-full max-w-md" @change=${this.onPipelineChange}>
          ${this.pipelineRuns.map(run => html`
            <option value=${run.pipelineId} ?selected=${run.pipelineId === this.selectedPipelineId}>
              ${formatPipelineLabel(run)}
            </option>
          `)}
        </select>
      </div>
      <ci-gantt-chart .data=${ganttData}></ci-gantt-chart>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'ci-gantt-panel': CiGanttPanel;
  }
}
