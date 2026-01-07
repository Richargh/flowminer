# Change Log - Visualization

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project might soon adhere to [Semantic Versioning](http://semver.org/)

## [unreleased] (Added 🚀 | Changed | Removed | Fixed 🐞 | Chore 👨‍💻 👩‍💻)

## [0.0.5] - 2026-01-07

### Chore 👨‍💻 👩‍💻

- **CHANGELOG**: Add changelog to track notable changes

## [0.0.3] - 2026-01-07

### Fixed 🐞

- Vite config base path so visualization can find assets

## [0.0.1] - 2026-01-07

### Added 🚀

- Initial visualization setup with:
  - Lit web components
  - Vite
  - Apache ECharts
- **Imports Commits** and visualizes them:
  - Work item scatter chart for analyzing work item duration
  - Theme switcher with dark/light mode support
  - File loader component for loading git mining results
  - Commit range filter at the top of the app
  - Commits table with sortable and filterable columns (type, work keys, branch)
  - Multi-select dropdown for type filtering in commit table
