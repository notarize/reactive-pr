package com.notarize.reactive.model

class ActionProcess(
    data: Map<*, *>
) {

    val label: List<String>

    val review: List<String>

    val teamReview: List<String>

    val comment: List<String>

    init {
        label = (data["label"] as? List<*>)?.map { it as String } ?: emptyList()
        review = (data["review"] as? List<*>)?.map { it as String } ?: emptyList()
        teamReview = (data["team_review"] as? List<*>)?.map { it as String } ?: emptyList()
        comment = (data["comment"] as? List<*>)?.map { it as String } ?: emptyList()
    }

    override fun toString(): String {
        return "Label: $label\nReview: $review\nTeam Review: $teamReview"
    }

}