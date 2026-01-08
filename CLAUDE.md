This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**ALWAYS** add the STARTER_CHARACTER followed by space at the start of your reply.
The starter character will change for each file that is in progress.

Default STARTER_CHARACTER = 0️⃣

## Repository Overview

This is a repo containing two distinct but interconnected parts:

* Analysis of data inside: 
  * When working in the analysis ALWAYS read the @analysis/A-AGENTS.md
  * Projects: @analysis/git-importer, @analysis/jira-importer
* Visualization of data
  * When working in the visualization ALWAYS read the @visualization/V-AGENTS.md

## AI Guides

ALWAYS generate the following `.md` files ALWAYS inside @.drafts.

In particular keep track of the three (sometimes four) documents when doing feature development:

| Document               | Purpose                                    | Updates                 |
|------------------------|--------------------------------------------|-------------------------|
| <feature>.research.md  | (Optional) Additional research             | As discoveries occur    |
| <feature>.spec.md      | What we want to build                      | Only with user approval |
| <feature>.plan.md      | How we'll get there from the current state | Constantly              |
| <feature>.learnings.md | What we discovered                         | As discoveries occur    |


In addition, store key architecture decisions as Architecture Decision Records (ADRs). Location @docs/adrs. ALWAYS suggest to write an ADR when we make an architecturally significant decision.

NEVER generate additional `.md` files beyond the ones mentioned above.

## CORE DEVELOPMENT PRINCIPLES

* Always follow the TDD cycle: Red → Green → Refactor
* Write the simplest failing test first
* Implement the minimum code needed to make tests pass
* Refactor only after tests are passing
* Maintain high code quality throughout development
* Employ strong types: create semantic wrappers for primitive types that describe what should be done. 

## TIDY FIRST APPROACH

* Separate all changes into two distinct types:
    1. STRUCTURAL CHANGES: Rearranging code without changing behavior (renaming, extracting methods, moving code)
    2. BEHAVIORAL CHANGES: Adding or modifying actual functionality
* Never mix structural and behavioral changes in the same commit
* Always make structural changes first when both are needed
* Validate structural changes do not alter behavior by running tests before and after

## CODE QUALITY STANDARDS

* Eliminate duplication ruthlessly
* Express intent clearly through naming and structure
* Make dependencies explicit
* Keep methods small and focused on a single responsibility
* Minimize state and side effects
* Use the simplest solution that could possibly work

## REFACTORING GUIDELINES

* Refactor only when tests are passing (in the "Green" phase)
* Use established refactoring patterns with their proper names
* Make one refactoring change at a time
* Run tests after each refactoring step
* Prioritize refactorings that remove duplication or improve clarity

## Development Commands

### Build Actions

* `just build` - Build the project
* `just dev-viz` - Start a dev visualization server

### Test Actions

* `just test` - Run all test suites
* `just test-analysis` - Run all analysis test suites
* `just test-viz` - Run all visualization test suites

To run a test for a subproject use:

* `just test-analysis-project <project>` - Run all test suites of the <project>. For example `just test-analysis-project analysis:gitcli`
* `just js-test-analysis-project <project>` - Run just the js tests of the <project>. For example `just js-test-analysis-project shared`
* `just jvm-test-analysis-project <project>` - Run just the jvm tests of the <project>. For example `just jvm-test-analysis-project analysis:git-importer`

## EXAMPLE WORKFLOW

When approaching a .plan.md:

1. Write a simple failing test for a small part of the plan
2. Implement the bare minimum to make it pass
3. Run tests to confirm they pass (Green)
4. Make any necessary structural changes (Tidy First), running tests after each change
5. Commit structural changes separately
6. Add another test for the next small increment of functionality
7. Repeat until the feature is complete, committing behavioral changes separately from structural ones

Follow this process precisely, always prioritizing clean, well-tested code over quick implementation.

Always write one test at a time, make it run, then improve structure. Always run all the tests (except long-running tests) each time.
