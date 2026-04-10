package de.richargh.flowminer.importer.gitlab.app.api

class GitLabRateLimitException(message: String = "GitLab API rate limit exceeded") : Exception(message)

class GitLabNotFoundException(projectId: String) : Exception("GitLab project not found: $projectId")

class GitLabAuthException(message: String = "GitLab authentication failed: check your token and permissions") : Exception(message)
