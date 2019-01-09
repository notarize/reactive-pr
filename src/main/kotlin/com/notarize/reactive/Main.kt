package com.notarize.reactive

import com.notarize.reactive.model.Comment


fun main() {
    val reactiveConfig = System.getProperties().parseReactiveConfig()
    val reactiveService = ReactiveService(
        reactiveConfig = reactiveConfig
    )
    if (reactiveService.reactive.value == null) {
        println("Reactive YAML not found")
        return
    }
    if (!reactiveService.isIssueOpen()) {
        println("Issue is not open: ${reactiveService.getIssueState()}")
        return
    }
    val passedReactions = reactiveService.getPassedReactions().toList()
    println("Passed Reactions:\n$passedReactions")
    passedReactions.map {
        it.actionProcess.label
    }
        .flatten()
        .toSet()
        .toList()
        .let {
            println("Matched Labels: $it")
            try {
                reactiveService.applyLabels(it)
            } catch (t: Throwable) {
                println("Failed to apply labels due to: ${t.localizedMessage}")
            }
        }
    passedReactions.map {
        it.actionProcess.review
    }
        .flatten()
        .toSet()
        .toList()
        .takeIf {
            it.isNotEmpty()
        }
        ?.let {
            println("Desired Reviewers: $it")
            try {
                reactiveService.assignReviewers(it)
            } catch (t: Throwable) {
                println("Failed to request reviews due to: ${t.localizedMessage}")
            }
        }
    passedReactions.map {
        it.actionProcess.teamReview
    }
        .flatten()
        .toSet()
        .toList()
        .takeIf {
            it.isNotEmpty()
        }
        ?.let {
            println("Desired Group Reviewers: $it")
            try {
                reactiveService.assignGroupReviewers(it)
            } catch (t: Throwable) {
                println("Failed to request group reviews due to: ${t.localizedMessage}")
            }

        }
    passedReactions.map {
        it.actionProcess.comment.map { comment ->
            Comment(
                reactionName = it.name,
                body = comment
            )
        }
    }
        .flatten()
        .toSet()
        .toList()
        .let {
            println("Desired Comments: $it")
            try {
                reactiveService.applyComments(it)
            } catch (t: Throwable) {
                println("Failed to apply comments due to: ${t.localizedMessage}")
            }
        }
}