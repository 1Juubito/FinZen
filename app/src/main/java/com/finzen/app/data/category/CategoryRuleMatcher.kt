package com.finzen.app.data.category

object CategoryRuleMatcher {

    data class Rule(val keyword: String, val categoryId: Long)

    fun match(description: String, rules: List<Rule>): Long? {
        if (description.isBlank()) return null
        val haystack = description.lowercase()
        return rules
            .filter { it.keyword.isNotBlank() && haystack.contains(it.keyword.trim().lowercase()) }
            .maxByOrNull { it.keyword.trim().length }
            ?.categoryId
    }
}
