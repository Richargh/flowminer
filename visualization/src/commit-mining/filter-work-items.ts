import type { WorkItem } from './app/api-types/work-items.ts';

export function filterWorkItemsByRange(
  workItems: WorkItem[],
  startDate: string,
  endDate: string
): WorkItem[] {
  const start = new Date(startDate).getTime();
  const end = new Date(endDate).getTime();

  return workItems.filter(item =>
    item.firstCommitDate >= start && item.firstCommitDate <= end
  );
}
