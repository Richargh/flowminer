import './commit-range/commit-range-panel.ts';
import './work-item-scatter/work-item-scatter-panel.ts';
import './work-item-duration-histogram/work-item-duration-panel.ts';
import './commits-table/commits-table-panel.ts';
import './theme-switcher/theme-switcher';
import './file-loading/file-loader.ts';
import './ci-pipeline/ci-file-loader.ts';
import './ci-pipeline/ci-boxplot-panel.ts';
import {loadDefaultData} from './startup/default-data-loader.ts';
import {loadDefaultCiData} from './startup/default-ci-data-loader.ts';

import type {GitMiningResult} from "./commit-mining/app/api-types/git-mining-result.ts";
import type {CiMiningResult} from './ci-pipeline/ci-file-loader.ts';

// Load default data at startup via the same event path as file loading
document.dispatchEvent(new CustomEvent<GitMiningResult>('data-loaded', {
  detail: loadDefaultData(),
  bubbles: true,
  composed: true
}));

document.dispatchEvent(new CustomEvent<CiMiningResult>('ci-data-loaded', {
  detail: loadDefaultCiData(),
  bubbles: true,
  composed: true
}));
