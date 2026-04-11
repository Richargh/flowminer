package de.richargh.flowminer.importer.jira.app.api

class JiraConnectionException(baseUrl: String, cause: Throwable) :
    Exception("Could not connect to Jira at $baseUrl: ${cause.message}", cause)

class JiraVersionDetectionException(baseUrl: String) :
    Exception("Could not detect a supported Jira API version at $baseUrl — is this a Jira instance?")

class JiraUnauthorizedException(message: String = "Invalid username or API token") : Exception(message)

class JiraForbiddenException(projectKey: String) :
    Exception("Access denied for project $projectKey: your account lacks the required permissions")

class JiraNotFoundException(projectKey: String) : Exception("Project $projectKey not found")

class JiraRateLimitException(message: String = "Jira API rate limit exceeded") : Exception(message)
