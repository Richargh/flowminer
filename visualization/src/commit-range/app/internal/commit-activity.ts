import type {Commit} from '../../../commit/app/api-types/commit.ts';

export interface CommitActivity {
    date: string;
    count: number;
}

export function aggregateCommitsByDate(commits: Commit[]): CommitActivity[] {
  const countsByDate = new Map<string, number>();

  for (const commit of commits) {
    const dateString = commit.date.toISOString().split('T')[0];
    const currentCount = countsByDate.get(dateString) ?? 0;
    countsByDate.set(dateString, currentCount + 1);
  }

  const result: CommitActivity[] = [];
  for (const [date, count] of countsByDate) {
    result.push({ date, count });
  }

  result.sort((a, b) => a.date.localeCompare(b.date));

  return result;
}
