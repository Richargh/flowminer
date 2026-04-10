/**
 * Pipeline job data as imported from a JSONL file.
 * Matches SerializablePipelineJobDto from the KMP model module.
 */
export interface PipelineJob {
  readonly pipelineId: number;
  readonly pipelineRef: string;
  readonly pipelineStatus: string;
  readonly pipelineCreatedAt: string;
  readonly jobId: number;
  readonly jobName: string;
  readonly jobStage: string;
  readonly jobStatus: string;
  readonly jobStartedAt: string | null;
  readonly jobFinishedAt: string | null;
  readonly jobDurationSeconds: number | null;
  readonly jobAllowFailure: boolean;
}

export interface BoxplotEntry {
  jobName: string;
  values: [number, number, number, number, number]; // [min, Q1, median, Q3, max]
}
