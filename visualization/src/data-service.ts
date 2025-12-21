import type {
  de as shared
} from 'teamcharta-shared';

// Extract only data properties from Kotlin DTOs (excluding copy, hashCode, equals, etc.)
type AuthorStatsDtoClass = shared.richargh.teamcharta.shared.dto.AuthorStatsDto;
type CommitTimelineDtoClass = shared.richargh.teamcharta.shared.dto.CommitTimelineDto;
type WorkItemDurationDtoClass = shared.richargh.teamcharta.shared.dto.WorkItemDurationDto;

// Re-export as plain data interfaces that match the DTO structure
export type AuthorStats = Pick<AuthorStatsDtoClass, 'name' | 'commitCount' | 'linesAdded' | 'linesDeleted' | 'avgCommitSize'>;
export type CommitTimeline = Pick<CommitTimelineDtoClass, 'date' | 'cumulativeCount' | 'author'>;
export type WorkItemDuration = Pick<WorkItemDurationDtoClass, 'key' | 'type' | 'startDate' | 'durationDays'>;

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
