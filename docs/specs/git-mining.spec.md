# Git-Importer

The following are to be implemented in a new file GitLogParser2.kt inside @analysis/git-importer. 

## Features

Returns a data class that answers the following questions:

* What branches exist or have existed?
  * How long have all these branches lived, i.e. what is the difference between the branch creation and when it was merged back?
* What issues where worked on in the git history?
  * An issue can be identified via the commit message. They are typically large three-letter numbers. Typical matching patterns are `#` followed by a couple of numbers and a couple of letters hyphen `-` and then some numbers. 
  * How long did the individual issues take, i.e. what is the time difference between the first commit with an issue number and the last commit?
  * An issue hierarchy is an optional input parameter. The hierarchy shows which issues belong to an epic and which subtask numbers belong to which issue. The issue hierarchy can be used to identify when work was done on a subtask of the actual issue.
  * How large where the individual issues, i.e. how much code was added/deleted/changed for the issue?
  * How many developers worked on this issue and how much did they change?
  * How many of the feature/fix/chore commits where made for this feature? The type of the commit can be identified from the commit message. A feature is a commit that contains the word `feat`, or `feature` with permutations with a double point `feat:`, square `feat[1234]` or `feat(123)`. A fix can likewise identified by keywords in the commit message `fix`, `bug`, or `hotfix`.
* Which versions exist?
  * Versions can be identified via numbered git tags. A version has often the tag `x.y`, `x.y.z` or even `x.y.z.w`. Sometimes this is prefixed by `v`. Sometimes the version gets a postfix like `-ga` for globally available or `-mx` for milestone x or `-preview` for a preview release. 
  * When was the version declared done, i.e. when was the most recent version tag made?
  * How many additions/deletions/changes where included in this version, i.e. how many lines have changed since the last version tag was made?
* What commits are in the git history?
  * On what day was the commit made, by which developer(s), how much was changed?
  * For which issue was the commit?
  * Was the commit a `feat`, a `fix` or unknown?
* What authors are in the git history?
  * How many commits has each individual author made?
  * How much code has each individual author added/deleted/changed?
  * How much deeply nested code has the author added/deleted/changed? 
  * With which other authors does this developer typically work with? Working with can be identified via a Co-Authored-by git trailer or because they work on the same issue.

The analysis time frame is configurable. By default it is the "last three months".