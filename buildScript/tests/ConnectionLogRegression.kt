package io.nekohasekai.sagernet.ui

fun main() {
    val inbound = "INFO[0001] [42 0ms] inbound/tun[tun-in]: inbound connection to 192.0.2.10:443"
    val log = """
        $inbound
        INFO[0001] [42 1ms] router: found package name: com.example.app, com.example.shared
        DEBUG[0001] [420 2ms] dns: selected example.com => unrelated
        DEBUG[0001] [42 2ms] router: pre-match[0] => route(preflight)
        DEBUG[0001] [42 3ms] router: sniffed protocol: tls, domain: example.com, client: example
        DEBUG[0001] [42 4ms] dns: match[1] => route(dns-proxy)
        DEBUG[0001] [42 4ms] dns: selected example.com => dns-proxy (default)
        DEBUG[0001] [42 5ms] router: match[4] rule_set=example => route(proxy)
        INFO[0001] [42 6ms] outbound/vless[Example exit]: outbound connection to example.com:443
        INFO[0001] [42 6ms] outbound/vless[Example exit]: outbound connection to example.com:443
        INFO[0001] [42 7ms] endpoint/wireguard[wg]: outbound packet connection to [2001:db8::1]:443
        ERROR[0001] [420 7ms] connection: unrelated error
        ERROR[0001] [42 8ms] connection: open connection to example.com:443: dial timeout
        INFO[0001] sing-box started
        DEBUG[0002] [43 11s] connection: connection upload finished
        INFO[0002] [44 0ms] dns: cached AAAA example.net. 42 IN AAAA 2001:db8::1
    """.trimIndent()

    check(inbound.substring(checkNotNull(ConnectionLog.idRange(inbound))) == "42")
    check(ConnectionLog.idRange("INFO[0001] sing-box started") == null)
    val details = ConnectionLog.inspect(log, "42")
    check(details.destination == "192.0.2.10:443")
    check(details.domain == "example.com")
    check(details.app == "com.example.app, com.example.shared")
    check(details.dns == listOf("selected example.com => dns-proxy (default)"))
    check(details.rules == listOf("match[4] rule_set=example => route(proxy)"))
    check(details.outbounds == listOf("vless[Example exit]", "wireguard[wg]"))
    check(details.errors == listOf("connection: open connection to example.com:443: dial timeout"))
    check(!details.transferFinished)
    check(details.lines.none { "[420 " in it })
    check(ConnectionLog.inspect(log, "43").transferFinished)
    check(ConnectionLog.inspect(log, "44").dns.single().startsWith("cached AAAA"))
    check(ConnectionLog.inspect(log, "45").lines.isEmpty())

    val longLog = (1..55).joinToString("\n") { "DEBUG[0001] [42 ${it}ms] router: match[$it] => route(proxy)" }
    val bounded = ConnectionLog.inspect(longLog, "42")
    check(bounded.lines.size == 40 && bounded.rules.size == 5)
    check("match[55]" in bounded.rules.last())
    println("Connection log regression passed")
}
