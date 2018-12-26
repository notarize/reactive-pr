package com.notarize.reactive.model

class Reactive(
    data: Map<*, *>
) {

    val reactions: List<Reaction> = (data["reactions"] as List<*>)
        .mapNotNull { it?.let { i -> Reaction(i as Map<*, *>) } }


    override fun toString(): String {
        return "Reactions: $reactions"
    }

}