# Change Log - Analysis

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project might soon adhere to [Semantic Versioning](http://semver.org/)

## [unreleased] (Added 🚀 | Changed | Removed | Fixed 🐞 | Chore 👨‍💻 👩‍💻)

### Chore 👨‍💻 👩‍💻

- **CHANGELOG**: Add changelog to track notable changes

## [0.0.2] - 2026-01-07

### Fixed 🐞

- **fmsh**: Reuse CommandSpec so tests can override System.Out

## [0.0.1] - 2026-01-07

### Added 🚀

- **FlowMiner Shell (fmsh)**: Interactive CLI shell for running analysis commands
- **GitHub CLI**: Command-line tool for importing github workitems:
  - Uses GraphQL API with streaming support
- **Git CLI**: Command-line tool for analyzing git repositories with:
  - Branch status tracking (active/stale/completed)
  - Work item analysis (grouping commits, churn metrics, collaborators)
  - Author statistics (commit counts, line changes, collaboration patterns)
  - JSON export capability
- **Git Log Mining**: Comprehensive commit analysis with:
  - Conventional commits parsing (feature, bugfix, refactor, test, docs, environment)
  - Risk-aware commits parsing (F, B, R, T, D, E)
  - Breaking change marker (!) support
  - Co-author and git trailer extraction
  - Work key detection from commit messages
  - Branch detection from commits (including inferred and unnamed branches)
  - Nested merge and octopus merge handling
  - HEAD detection and prioritization
