import sampleData from './default.tc.jsonl?raw';
import { parseJsonlLines } from '../commit/app/commit-file-parser.ts';
import { CommitMiner } from '../commit-mining/app/commit-miner.ts';
import type { GitMiningResult } from '../commit-mining/app/api-types/git-mining-result.ts';

export function loadDefaultData(): GitMiningResult {
    const miner = new CommitMiner();

    for (const commit of parseJsonlLines(sampleData)) {
        miner.process(commit);
    }

    return miner.getResult(Date.now());
}
