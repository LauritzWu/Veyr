package io.nekohasekai.sagernet.ui

internal object ConnectionLog {
    private val event = Regex("""^(TRACE|DEBUG|INFO|WARN|ERROR|FATAL|PANIC)\[\d+] \[(\d+) [^\]]+] (.+)$""")
    private val inboundDestination = Regex("""^inbound/.*?: .*?\binbound (?:packet )?connection to (.+)$""")
    private val sniffedDomain = Regex("""^router: sniffed protocol: [^,]+, domain: ([^,]+)(?:, .+)?$""")
    private val outbound = Regex("""^(?:outbound|endpoint)/([^\s:\[]+(?:\[.*?])?): outbound .*?\bconnection to (.+)$""")
    private val errorLevels = setOf("ERROR", "FATAL", "PANIC")
    private const val summaryLimit = 5
    private const val detailLimit = 40

    data class Details(
        val destination: String?,
        val domain: String?,
        val app: String?,
        val dns: List<String>,
        val rules: List<String>,
        val outbounds: List<String>,
        val errors: List<String>,
        val transferFinished: Boolean,
        val lines: List<String>,
    )

    fun idRange(line: String): IntRange? = event.matchEntire(line)?.groups?.get(2)?.range

    fun inspect(log: String, id: String): Details {
        val entries = log.lineSequence()
            .mapNotNull { line -> event.matchEntire(line)?.takeIf { it.groupValues[2] == id } }
            .toList()
        val messages = entries.map { it.groupValues[3] }
        val outboundEvents = messages.mapNotNull(outbound::matchEntire)
        val dnsMessages = messages.filter { it.startsWith("dns: ") }.map { it.removePrefix("dns: ") }
        return Details(
            destination = messages.firstNotNullOfOrNull { inboundDestination.matchEntire(it)?.groupValues?.get(1) }
                ?: outboundEvents.firstOrNull()?.groupValues?.get(2),
            domain = messages.firstNotNullOfOrNull { sniffedDomain.matchEntire(it)?.groupValues?.get(1) },
            app = messages.firstOrNull { it.startsWith("router: found package name: ") }
                ?.removePrefix("router: found package name: ")?.takeIf { it.isNotBlank() },
            dns = dnsMessages.filter { it.startsWith("selected ") }.ifEmpty {
                dnsMessages.filterNot { it.startsWith("match[") }
            }.distinct().takeLast(summaryLimit),
            rules = messages.filter { it.startsWith("router: match[") }
                .map { it.removePrefix("router: ") }.distinct().takeLast(summaryLimit),
            outbounds = outboundEvents.map { it.groupValues[1] }
                .distinct().takeLast(summaryLimit),
            errors = entries.filter { it.groupValues[1] in errorLevels }
                .map { it.groupValues[3] }.distinct().takeLast(summaryLimit),
            transferFinished = messages.any {
                it == "connection: connection upload finished" || it == "connection: connection download finished"
            },
            lines = entries.takeLast(detailLimit).map { it.value },
        )
    }
}
