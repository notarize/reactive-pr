package com.notarize.reactive.model

class RuleCheck(
    data: Map<*, *>
) {

    val added: List<String>

    val modified: List<String>

    init {
        added = (data["added"] as? List<*>)?.map { it as String } ?: emptyList()
        modified = (data["modified"] as? List<*>)?.map { it as String } ?: emptyList()
    }

    override fun toString(): String {
        return "Added: $added\nModified: $modified"
    }

}