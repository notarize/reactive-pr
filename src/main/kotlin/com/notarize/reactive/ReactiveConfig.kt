package com.notarize.reactive

data class ReactiveConfig(
    val githubAuthToken: String,
    val repositoryName: String,
    val baseSha: String,
    val headSha: String,
    val issueNumber: Int
)