package moe.matsuri.nb4a.proxy.config

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser

internal fun fullSingBoxConfig(contents: String): JsonObject? {
    return parseJsonObject(contents)?.takeIf { root ->
        root.get("inbounds")?.isJsonArray == true &&
            (root.get("outbounds")?.isJsonArray == true || root.get("endpoints")?.isJsonArray == true)
    }
}

internal fun fullSingBoxNodeNames(contents: String): List<String> {
    val config = fullSingBoxConfig(contents) ?: return emptyList()
    val nonNodeTypes = setOf("direct", "block", "dns", "selector", "urltest")
    return listOf("outbounds", "endpoints")
        .flatMap { key -> config.get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.toList().orEmpty() }
        .mapNotNull { it.takeIf { value -> value.isJsonObject }?.asJsonObject }
        .mapNotNull { node ->
            val type = node.stringValue("type")
            if (type == null || type in nonNodeTypes) null
            else node.stringValue("tag")?.takeIf(String::isNotBlank)
        }
        .distinct()
}

private fun JsonObject.stringValue(key: String): String? =
    get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString


internal data class FullSingBoxSelector(
    val tag: String,
    val members: List<String>,
    val default: String,
)

internal fun fullSingBoxSelectors(contents: String): List<FullSingBoxSelector> {
    val config = fullSingBoxConfig(contents) ?: return emptyList()
    return config.selectors()
}

private fun JsonObject.selectors(): List<FullSingBoxSelector> =
    get("outbounds")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { element ->
        val group = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
        if (group.stringValue("type") != "selector") return@mapNotNull null
        val tag = group.stringValue("tag")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        val members = group.get("outbounds")?.takeIf { it.isJsonArray }?.asJsonArray
            ?.mapNotNull { it.takeIf { value -> value.isJsonPrimitive && value.asJsonPrimitive.isString }?.asString }
            ?.filter(String::isNotBlank)?.distinct().orEmpty()
        val first = members.firstOrNull() ?: return@mapNotNull null
        FullSingBoxSelector(tag, members, group.stringValue("default")?.takeIf { it in members } ?: first)
    }.orEmpty()

internal fun fullSingBoxSelectorChoices(contents: String, choices: String?): Map<String, String> {
    val saved = choices?.let(::parseJsonObject) ?: return emptyMap()
    return fullSingBoxSelectors(contents).mapNotNull { group ->
        saved.stringValue(group.tag)?.takeIf { it in group.members }?.let { group.tag to it }
    }.toMap()
}

internal data class FullSingBoxDetails(
    val finalOutbound: String?,
    val selectors: List<Pair<FullSingBoxSelector, String>>,
    val dnsServers: List<String>,
    val finalDnsServer: String?,
    val ruleSets: List<String>,
    val routeRuleCount: Int,
)

internal fun fullSingBoxDetails(contents: String, choices: String?): FullSingBoxDetails? {
    val config = fullSingBoxConfig(contents) ?: return null
    val saved = choices?.let(::parseJsonObject)
    val route = config.get("route")?.takeIf { it.isJsonObject }?.asJsonObject
    val dns = config.get("dns")?.takeIf { it.isJsonObject }?.asJsonObject
    return FullSingBoxDetails(
        finalOutbound = route?.stringValue("final")?.takeIf(String::isNotEmpty)
            ?: config.firstEntryTag("outbounds"),
        selectors = config.selectors().map { group ->
            group to (saved?.stringValue(group.tag)?.takeIf { it in group.members } ?: group.default)
        },
        dnsServers = dns?.taggedEntries("servers").orEmpty(),
        finalDnsServer = dns?.stringValue("final")?.takeIf(String::isNotEmpty)
            ?: dns?.firstEntryTag("servers"),
        ruleSets = route?.taggedEntries("rule_set").orEmpty(),
        routeRuleCount = route?.get("rules")?.takeIf { it.isJsonArray }?.asJsonArray?.size() ?: 0,
    )
}

private fun JsonObject.firstEntryTag(key: String): String? {
    val first = get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.firstOrNull()
    return first?.takeIf { it.isJsonObject }?.asJsonObject?.stringValue("tag")?.takeIf(String::isNotBlank)
}

private fun JsonObject.taggedEntries(key: String): List<String> =
    get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { element ->
        val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
        val tag = item.stringValue("tag")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        val type = item.stringValue("type")?.takeIf(String::isNotBlank)
        if (type == null) tag else "$tag ($type)"
    }.orEmpty()

private fun parseJsonObject(contents: String): JsonObject? = try {
    JsonParser.parseString(contents).takeIf { it.isJsonObject }?.asJsonObject
} catch (_: JsonParseException) {
    null
}
