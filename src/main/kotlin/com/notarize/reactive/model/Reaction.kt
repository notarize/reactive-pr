package com.notarize.reactive.model

class Reaction(
    data: Map<*, *>
) {

    val name: String

    val rules: Map<RuleResult, RuleCheck>

    val actionProcess: ActionProcess

    init {
        println(data)
        name = (data["name"] as? String).orEmpty()
        rules = (data["rules"] as Map<*, *>).let { rules ->
            val map = mutableMapOf<RuleResult, RuleCheck>()
            rules.forEach { ruleEntry ->
                val ruleResult = when (ruleEntry.key) {
                    "truths" -> RuleResult.Truths
                    "lies" -> RuleResult.Lies
                    else -> throw IllegalArgumentException("Unexpected rule result: ${ruleEntry.key}")
                }
                map[ruleResult] = RuleCheck(
                    data = ruleEntry.value as Map<*, *>
                )
            }
            return@let map
        }
        actionProcess = ActionProcess(
            data = data["actions"] as Map<*, *>
        )
    }

    override fun toString(): String {
        return "Name: $name\nRules: ${rules.map {
            "${it.key.name} -> ${it.value}"
        }}\nActionProcess: $actionProcess"
    }

}