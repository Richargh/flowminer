import sampleData from './default-ci.jsonl?raw';
import type { CiMiningResult } from '../ci-pipeline/ci-file-loader.ts';
import type { PipelineJob } from '../ci-pipeline/ci-types.ts';

export function loadDefaultCiData(): CiMiningResult {
    const jobs: PipelineJob[] = [];
    for (const line of sampleData.split('\n')) {
        if (line.trim().length === 0) continue;
        try {
            jobs.push(JSON.parse(line) as PipelineJob);
        } catch {
            console.warn('Skipping malformed JSONL line:', line);
        }
    }
    return { jobs };
}
