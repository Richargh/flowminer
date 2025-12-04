This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**ALWAYS** add the STARTER_CHARACTER followed by space at the start of your reply.
The starter character will change for each `*.research.md`, `*.plan.md` or `*.process.md` file that is in progress.

Default STARTER_CHARACTER = 🏃

## Repository Overview

This is a repo containing two distinct but interconnected parts:

* Analysis of data: @git-importer, @jira-importer
  * When working in the analysis ALWAYS read the @analysis/CLAUDE.md
* Visualization of data
  * When working in the visualization ALWAYS read the @visualization/CLAUDE.md

## AI Guides

* Generate `.md` files ALWAYS inside @docs for:
  * Specs, that describe a new feature that we want to build, with the suffix `.spec.md`. Location @docs/specs.
  * Research, that describe the state the app is currently in before implementing the spec, with the suffix `.research.md`. Location @docs/research.
  * Plans, that describe how to implement a spec given the state described in the research, with the suffix `.plan.md`. Location @docs/plans.
  * Process, that describe general processes to follow and are read on demand, with the suffix `.process.md`. Location @docs/processes.
    * See this folder when needing to take an action like: "Write an ADR" or "Make a commit".
  * Architecture Decision Records (ADRs), that describe key architecture decisions that were made. Location @docs/adrs.
* Keep plans simple and concise - avoid over-elaboration, detailed sections, or comprehensive documentation style
* Plans should be brief, actionable outlines rather than detailed specifications
* ALWAYS suggest to write an ADR when we make an architecturally significant decision.
* NEVER generate additional `.md` files

## CORE DEVELOPMENT PRINCIPLES

* Always follow the TDD cycle: Red → Green → Refactor
* Write the simplest failing test first
* Implement the minimum code needed to make tests pass
* Refactor only after tests are passing
* Maintain high code quality throughout development

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

## EXAMPLE WORKFLOW

When approaching a plan:

1. Write a simple failing test for a small part of the plan
2. Implement the bare minimum to make it pass
3. Run tests to confirm they pass (Green)
4. Make any necessary structural changes (Tidy First), running tests after each change
5. Commit structural changes separately
6. Add another test for the next small increment of functionality
7. Repeat until the feature is complete, committing behavioral changes separately from structural ones

Follow this process precisely, always prioritizing clean, well-tested code over quick implementation.

Always write one test at a time, make it run, then improve structure. Always run all the tests (except long-running tests) each time.
