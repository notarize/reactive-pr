package com.notarize.reactive

const val GITHUB_TOKEN_ARG = "reactive.github.token"
const val GITHUB_REPO_ARG = "reactive.github.repo"
const val GITHUB_ISSUE_ARG = "reactive.github.issue"
const val GITHUB_BASE_ARG = "reactive.github.base"
const val GITHUB_HEAD_ARG = "reactive.github.head"

fun Map<Any, Any>.parseReactiveConfig(): ReactiveConfig {
    val authToken = get(GITHUB_TOKEN_ARG)?.toString()
        ?: throw IllegalStateException("Missing Github token option named as $GITHUB_TOKEN_ARG")
    val repoName = get(GITHUB_REPO_ARG)?.toString()
        ?: throw IllegalStateException("Missing Github repo name option named as $GITHUB_REPO_ARG")
    val issueNumber = get(GITHUB_ISSUE_ARG)?.toString()?.toIntOrNull()
        ?: throw IllegalStateException("Missing Github issue number option named as $GITHUB_ISSUE_ARG")
    val baseSha = get(GITHUB_BASE_ARG)?.toString()
        ?: throw IllegalStateException("Missing Github base sha option named as $GITHUB_BASE_ARG")
    val headSha = get(GITHUB_HEAD_ARG)?.toString()
        ?: throw IllegalStateException("Missing Github head sha option named as $GITHUB_HEAD_ARG")
    return ReactiveConfig(
        githubAuthToken = authToken,
        repositoryName = repoName,
        issueNumber = issueNumber,
        baseSha = baseSha,
        headSha = headSha
    )
}