# Veyr for Android

Veyr 是基于 sing-box 的 Android 代理客户端，支持 Sub-Store 订阅、规则集与 DNS 分流，以及可搜索的连接日志。

## 下载 Veyr

| 渠道 | Android ARM64 安装包 | 版本说明 |
| --- | --- | --- |
| 稳定版 | [下载 1.0.10 稳定版 APK](https://github.com/LauritzWu/Veyr/releases/download/v1.0.10-stable.25940/veyr-1.0.10-stable-arm64-v8a.apk) | [最新稳定版](https://github.com/LauritzWu/Veyr/releases/latest) |
| Beta 版 | [下载 1.0.10-BETA.3 APK](https://github.com/LauritzWu/Veyr/releases/download/v1.0.10-beta.3.25935/veyr-1.0.10-beta.3-arm64-v8a.apk) | [所有 Beta 发布](https://github.com/LauritzWu/Veyr/releases?q=prerelease%3Atrue) |

已安装 Veyr 的用户可在 **关于 → 检查更新** 选择稳定版或 Beta 版。覆盖安装时直接打开下载的 APK。
## 支持的代理协议 / Supported Proxy Protocols

* SOCKS (4/4a/5)
* HTTP(S)
* SSH
* Shadowsocks
* ShadowsocksR
* VMess
* Trojan
* VLESS
* AnyTLS/AnyReality
* Snell 1/2/3/4/5/6
* ShadowTLS
* TUIC
* Juicity
* Hysteria 1/2
* WireGuard
* Trojan-Go (trojan-go-plugin)
* NaïveProxy (naive-plugin)
* Mieru (mieru-plugin)

<details>
<summary>XHTTP Extra TLS配置示例</summary>

<pre><code class="language-json">
{
    "no_grpc_header": false,  // stream-up/one
	"x_padding_bytes": "100-10000",
	"sc_max_each_post_bytes": 1000000, // packet-up only
	"sc_min_posts_interval_ms": 30, // packet-up only
	"xmux": {
		"max_concurrency": "16-32",
		"max_connections": "0-0",
		"c_max_reuse_times": "0-0",
		"h_max_request_times": "600-900",
		"h_max_reusable_secs": "1800-3000",
		"h_keep_alive_period": 0
	},
    "x_padding_obfs_mode": false,
    "x_padding_key": "",
    "x_padding_header": "",
    "x_padding_placement": "",
    "x_padding_method": "",
    "uplink_http_method": "",
    "session_placement": "",
    "session_key": "",
    "session_id_table": "",  // hex/base36/base62/alphabet etc, ""=UUID
    "session_id_length": 0,
    "seq_placement": "",
    "seq_key": "",
    "uplink_data_placement": "",
    "uplink_data_key": "",
    "uplink_chunk_size": 0,
    "congestion_controller": "",  // H3 only: ""=bbr, bbr/cubic/reno
    "cwnd": 0,  // H3 only, default 32
	"download": {
		"mode": "auto",
		"host": "b.yourdomain.com",
		"path": "/xhttp",
        "no_grpc_header": false,  // stream-up/one
	    "x_padding_bytes": "100-10000",
	    "sc_max_each_post_bytes": 1000000, // packet-up only
	    "sc_min_posts_interval_ms": 30, // packet-up only
		"xmux": {
			"max_concurrency": "16-32",
			"max_connections": "0-0",
			"c_max_reuse_times": "0-0",
			"h_max_request_times": "600-900",
			"h_max_reusable_secs": "1800-3000",
			"h_keep_alive_period": 0
		},
        "x_padding_obfs_mode": false,
        "x_padding_key": "",
        "x_padding_header": "",
        "x_padding_placement": "",
        "x_padding_method": "",
        "uplink_http_method": "",
        "session_placement": "",
        "session_key": "",
        "session_id_table": "",  // hex/base36/base62/alphabet etc, ""=UUID
        "session_id_length": 0,
        "seq_placement": "",
        "seq_key": "",
        "uplink_data_placement": "",
        "uplink_data_key": "",
        "uplink_chunk_size": 0,
        "congestion_controller": "",  // H3 only: ""=bbr, bbr/cubic/reno
        "cwnd": 0,  // H3 only, default 32
		"server": "$(ip_or_domain_of_your_cdn)",
		"server_port": 443,
		"tls": {
			"enabled": true,
			"server_name": "b.yourdomain.com",
			"alpn": "h2",
			"utls": {
				"enabled": true,
				"fingerprint": "chrome"
			}
		}
	}
}
</code></pre>
</details>

<details>
<summary>XHTTP Extra Reality配置示例</summary>

<pre><code class="language-json">
{
    "no_grpc_header": false,  // stream-up/one
	"x_padding_bytes": "100-10000",
	"sc_max_each_post_bytes": 1000000, // packet-up only
	"sc_min_posts_interval_ms": 30, // packet-up only
	"xmux": {
		"max_concurrency": "16-32",
		"max_connections": "0-0",
		"c_max_reuse_times": "0-0",
		"h_max_request_times": "600-900",
		"h_max_reusable_secs": "1800-3000",
		"h_keep_alive_period": 0
	},
    "x_padding_obfs_mode": false,
    "x_padding_key": "",
    "x_padding_header": "",
    "x_padding_placement": "",
    "x_padding_method": "",
    "uplink_http_method": "",
    "session_placement": "",
    "session_key": "",
    "session_id_table": "",  // hex/base36/base62/alphabet etc, ""=UUID
    "session_id_length": 0,
    "seq_placement": "",
    "seq_key": "",
    "uplink_data_placement": "",
    "uplink_data_key": "",
    "uplink_chunk_size": 0,
    "congestion_controller": "",  // H3 only: ""=bbr, bbr/cubic/reno
    "cwnd": 0,  // H3 only, default 32
	"download": {
		"mode": "auto",
		"host": "example.com",
		"path": "/xhttp",
        "no_grpc_header": false,  // stream-up/one
	    "x_padding_bytes": "100-10000",
	    "sc_max_each_post_bytes": 1000000, // packet-up only
	    "sc_min_posts_interval_ms": 30, // packet-up only
		"xmux": {
			"max_concurrency": "16-32",
			"max_connections": "0-0",
			"c_max_reuse_times": "0-0",
			"h_max_request_times": "600-900",
			"h_max_reusable_secs": "1800-3000",
			"h_keep_alive_period": 0
		},
        "x_padding_obfs_mode": false,
        "x_padding_key": "",
        "x_padding_header": "",
        "x_padding_placement": "",
        "x_padding_method": "",
        "uplink_http_method": "",
        "session_placement": "",
        "session_key": "",
        "session_id_table": "",  // 可选: hex/base36/base62/alphabet 等, 留空则用 UUID
        "session_id_length": 0,
        "seq_placement": "",
        "seq_key": "",
        "uplink_data_placement": "",
        "uplink_data_key": "",
        "uplink_chunk_size": 0,
        "congestion_controller": "",  // H3 only: ""=bbr, bbr/cubic/reno
        "cwnd": 0,  // H3 only, default 32
		"server": "$(ip_or_domain_of_your_cdn)",
		"server_port": 443,
		"tls": {
			"enabled": true,
			"server_name": "example.com",
			"reality": {
				"enabled": true,
				"public_key": "$(your_publicKey)",
				"short_id": "$(your_shortId)"
			},
			"utls": {
				"enabled": true,
				"fingerprint": "chrome"
			}
		}
	}
}
</code></pre>
</details>

## 支持的订阅格式 / Supported Subscription Format

* 一些广泛使用的格式 (如 Shadowsocks, ClashMeta 和 v2rayN)
* sing-box 出站

仅支持解析出站，即节点。分流规则等信息会被忽略。

* Some widely used formats (like Shadowsocks, ClashMeta and v2rayN)
* sing-box outbound

Only resolving outbound, i.e. nodes, is supported. Information such as diversion rules are ignored.

## Credits

Core:

- [SagerNet/sing-box](https://github.com/SagerNet/sing-box)

Android GUI:

- [shadowsocks/shadowsocks-android](https://github.com/shadowsocks/shadowsocks-android)
- [SagerNet/SagerNet](https://github.com/SagerNet/SagerNet)

Web Dashboard:

- [Yacd-meta](https://github.com/MetaCubeX/Yacd-meta)

## 来源与许可

Veyr 基于 NekoBox for Android 及其上游 SagerNet 持续开发。原项目和贡献者的版权声明见 [AUTHORS](AUTHORS) 和 [LICENSE](LICENSE)。

- [MatsuriDayo/NekoBoxForAndroid](https://github.com/MatsuriDayo/NekoBoxForAndroid)
- [starifly/NekoBoxForAndroid](https://github.com/starifly/NekoBoxForAndroid)
