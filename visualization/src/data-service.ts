import type {AuthorStatsDto, CommitTimelineDto, WorkItemDurationDto} from 'flowminer-shared';

// Re-export as plain data interfaces that match the DTO structure (excluding copy, hashCode, equals, etc.)
export type AuthorStats = Pick<AuthorStatsDto, 'name' | 'commitCount' | 'linesAdded' | 'linesDeleted' | 'avgCommitSize'>;
export type CommitTimeline = Pick<CommitTimelineDto, 'date' | 'cumulativeCount' | 'author'>;
export type WorkItemDuration = Pick<WorkItemDurationDto, 'key' | 'type' | 'startDate' | 'durationDays'>;

export interface SankeyNode {
  name: string;
}

export interface SankeyLink {
  source: string;
  target: string;
  value: number;
}

export interface SankeyFlow {
  nodes: SankeyNode[];
  links: SankeyLink[];
}

export interface HistogramBucket {
  range: string;
  count: number;
  minValue: number;
  maxValue: number;
}

export interface VisualizationData {
  authors: AuthorStats[];
  commitTimeline: CommitTimeline[];
  workItems: WorkItemDuration[];
  sankeyFlow?: SankeyFlow;
  histogram?: HistogramBucket[];
}

interface RawVisualizationData {
  authors?: unknown[];
  commitTimeline?: unknown[];
  workItems?: unknown[];
}

export function loadVisualizationData(rawData: RawVisualizationData): VisualizationData {
  return {
    authors: (rawData.authors ?? []) as AuthorStats[],
    commitTimeline: (rawData.commitTimeline ?? []) as CommitTimeline[],
    workItems: (rawData.workItems ?? []) as WorkItemDuration[],
  };
}
