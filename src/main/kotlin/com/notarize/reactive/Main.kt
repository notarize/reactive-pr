package com.notarize.reactive


fun main() {
    val reactiveConfig = System.getProperties().parseReactiveConfig()
    val reactiveService = ReactiveService(
        reactiveConfig = reactiveConfig
    )
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
        .takeIf {
            it.isNotEmpty()
        }
        ?.let {
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
}