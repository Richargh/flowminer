import type { WorkItem } from './app/api-types/work-items.ts';

export function filterWorkItemsByRange(
  workItems: WorkItem[],
  startDate: string,
  endDate: string
): WorkItem[] {
  const start = new Date(startDate);
  const end = new Date(endDate);

  return workItems.filter(item =>
    item.firstCommitDate >= start && item.firstCommitDate <= end
  );
}
