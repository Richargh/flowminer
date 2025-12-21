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

export interface VisualizationData {
  authors: AuthorStats[];
  commitTimeline: CommitTimeline[];
  workItems: WorkItemDuration[];
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
