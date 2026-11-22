# Veyr Android 开发状态与路线图

最近核对：2026-11-19。本文记录 GitHub 仓库、构建产物和可复核的测试证据。

## 1. 项目基本信息

Veyr 是 Starifly NekoBoxF 的 Android fork，Starifly NekoBoxF 源自 MatsuriDayo NekoBox for Android。SagerNet 维护 sing-box 核心及其官方 Android 客户端。功能覆盖 Sub-Store 订阅、路由规则、DNS 分流和连接日志。[仓库 README](README.md) · [许可证](LICENSE)

| 路径 | 用途 |
| --- | --- |
| `app/` | Kotlin Android 客户端、UI、语言资源和 APK 打包 |
| `libcore/`、`hev-socks5-tunnel/` | Go 原生核心、sing-box 集成和 TUN 实现 |
| `buildSrc/`、`buildScript/`、`gradle/` | 构建约定、脚本与 Gradle Wrapper |
| `nb4a.properties` | 基础版本和包名；CI 对候选构建覆盖这些值 |
| `.github/workflows/` | 构建、产物校验和发布流程 |

GitHub 默认分支为 `lauritz/starifly-core-1.14`（`62731eb`），应用诊断开发分支为 `feature/app-diagnostics`（Beta.3 源码 `6720338`，包含已验证的 `396b98e`）。`main` 也是现存分支。核对时仓库没有可见的 Pull Requests 或 Issues；下文的问题来自实机反馈与代码核对。[分支](https://github.com/LauritzWu/Veyr/branches) · [Pull Requests](https://github.com/LauritzWu/Veyr/pulls) · [Issues](https://github.com/LauritzWu/Veyr/issues)

## 2. 当前版本与构建状态

`1.0.10-STABLE`（25940）實機驗收通過，已正式發布。[發布頁](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.10-stable.25940)

| 渠道 | 显示版本 | Android versionCode | 构建与源码 |
| --- | --- | ---: | --- |
| GitHub 最新稳定版 | 1.0.10-STABLE | 25940 | ARM64，签名构建与实机验收通过，[发布页](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.10-stable.25940) |
| GitHub 历史稳定版 | 1.0.9-STABLE | 25920 | ARM64，签名构建与实机升级通过 |
| GitHub 历史稳定版 | 1.0.6-STABLE | 25880 | ARM64，S24 Ultra 实机与发布校验通过 |
| GitHub 历史稳定版 | 1.0.5-STABLE | 25865 | ARM64，`42c5bb5`，S24 Ultra 验收及发布校验通过 |
| GitHub 历史稳定版 | 1.0.4-STABLE | 25840 | ARM64，`c7b112b`，S24 Ultra 验收及发布校验通过 |
| GitHub 历史稳定版 | 1.0.3-STABLE | 25810 | ARM64，`5d4537f`，Release 构建，CI 与实机验证已通过 |
| Actions 前一稳定候选 | 1.0.3-STABLE | 25800 | ARM64，`539f754`，实测覆盖安装及配置保留通过，「关于」页显示 DEBUG |
| Actions 前一稳定候选 | 1.0.3 DEBUG | 25780 | ARM64，`83880d5`，繁中频道文案已通过实机验收 |
| Actions 前一稳定候选 | 1.0.3 DEBUG | 25770 | ARM64，`68de6ad`，CI 签名校验和实机覆盖安装通过 |
| GitHub 历史稳定版 | 1.0.2 | 25685 | ARM64，`dc73a2c` |
| GitHub 预发布版 | 1.0.3-beta.3 DEBUG | 25745 | ARM64，`6720338`，应用内升级已通过实机验证 |
| GitHub 历史预发布版 | 1.0.3-beta.2 DEBUG | 25715 | ARM64，`712445f`，applicationId 为 `top.lauritz.jc.uipreview.debug` |
| Actions 签名测试包 | 1.0.3-beta.2 DEBUG | 25725 | ARM64，`396b98e`，已通过实机回归 |

开发分支的 `nb4a.properties` 基础值为 `VERSION_NAME=1.0.1-beta.1`、`VERSION_CODE=46`。CI #43 覆盖版本与包名，再执行 `app:assembleOssDebug`。ARM64 APK 的版本号、签名与对齐均由 CI 校验。该流程使用 JDK 17、Android SDK Platform 35、Build Tools 35.0.1 和 NDK 25.0.8775105。实机“关于”页显示 sing-box `1.14.4-neko-1`。

## 3. 已实现功能清单

| 功能 | 当前证据 |
| --- | --- |
| 按应用查看近期连接、繁中诊断界面 | 代码已实现，25715 的 YouTube 和 Chrome Beta 用例已通过 |
| 按域名查看近期 DNS、规则命中与出站 | 稳定版发布说明已记录；25715 的独立域名诊断入口待专门回归 |
| 日志搜索和高亮 | 提交 `7bd0d1a` 已实现；25715 的专项测试待补 |
| 订阅、节点、DNS、路由规则 | README 与源码已有对应功能；覆盖安装已核对已有资料，订阅刷新及完整 DNS／规则矩阵待验证 |
| GitHub 稳定／Beta 更新检查 | 代码已实现；25715 复现识别问题，25725 已通过渠道识别与无降级提示回归 |

## 4. 已验证的测试结果

| 用例 | 版本 | 结果与证据 |
| --- | --- | --- |
| 繁中诊断与版本显示 | 25715 | 通过。截图显示繁中“关于”与“记录档”，版本为 1.0.3-beta.2 DEBUG |
| YouTube 应用归属与代理出站 | 25715 | 通过。应用包名 `com.google.android.youtube`，`i.ytimg.com`、`googlevideo.com` 相关流量显示经洛杉矶 VLESS 出站 |
| Chrome Beta 应用归属 | 25715 | 通过。日志归属 `com.android.chrome`，含 `example.com`；`example.org` 记录显示洛杉矶 VLESS 出站 |
| Wi-Fi ↔ 行动网络双向切换 | 25715 | 通过。维护者确认两次切换后均能打开网页，且有新连接日志 |
| 覆盖安装与资料保留 | 25685 → 25715 | 通过。维护者确认直接覆盖安装后，原有订阅、节点、路由规则均保留 |
| 已装新版本时检查稳定渠道 | 25715 | 通过该用例。界面显示已安装 25715、可用 25685、当前渠道没有更新 |
| Beta 渠道发现预发布版 | 25715 | 失败。维护者确认 Beta 也显示可用 25685；截图记录对应提示，源码核对确认 25715 会被筛掉 |
| 检查更新与无降级提示 | 25725 | 通过。维护者确认 Beta 显示可用 25715，稳定渠道显示可用 25685，两个渠道均显示没有更新 |
| Beta 渠道端到端升级 | 25725 → 25745 | 通过。维护者确认从应用内 Beta 渠道下载并覆盖安装，原有订阅、节点和路由规则均保留 |
| 稳定候选覆盖安装与更新检查 | 25745 → 25770 | 通过。订阅、节点和路由规则保留；稳定渠道可用 25685、Beta 渠道可用 25745，均提示没有更新 |
| 检查更新渠道识别 | 25745 | 通过。Beta 渠道识别 25745；稳定渠道显示已安装 25745、可用 25685 |
| 覆盖安装与应用诊断回归 | 25715 → 25725 | 通过。维护者确认订阅、节点及路由规则保留，YouTube 和 Chrome 的连接归属正常 |

## 5. 已知 Bug 与待修复问题

**P1 · Beta 更新渠道识别不到已发布的 25715（已修复并验收）。** `AboutFragment.kt` 仅接受以 `-preview.<build>` 或 `-stable.<build>` 结尾的标签，当前预发布标签为 `v1.0.3-beta.2.25715`。预发布 APK 的文件名也不在更新器的 ARM64 资产白名单中。两项筛选都会导致该发布被跳过。稳定渠道排除预发布版符合本次预期。修复已提交至诊断分支 [396b98e](https://github.com/LauritzWu/Veyr/commit/396b98ef2462ff1ce949dcc636486158857b79ed)，保留旧标签和 APK 名称，并按 Beta 标签识别版本对应的 ARM64 安装包。根据 GitHub Release 数据核对筛选结果，Beta 选中 25715，稳定渠道选中 25685。25725 已通过 Android 实机回归。25725 → 25745 的 Beta 渠道发现、下载和覆盖安装已通过实机验证。

- [x] 核对标签和资产命名约定，并提交兼容历史版本的修复。
- [x] 实机验证 25725 的 Beta／稳定渠道识别和无降级提示。
- [x] 实机验证 25725 能发现更高 versionCode 的 Beta（25745）。
- [x] 通过应用内 Beta 下载入口取得并安装 25745 ARM64 APK。

## 6. 待开发功能与优先级

| 优先级 | 尚未交付项 | 验收标准 |
| --- | --- | --- |
| P2 | 更新候选筛选的自动化回归 | 覆盖渠道、标签、资产名称和 versionCode 比较 |

## 7. 下一阶段开发路线图

- [x] 复现并修复更新候选筛选。
- [x] 用 25725 回归更新检查、应用诊断及覆盖安装。
- [x] 核对 25745 的 Beta 和稳定渠道识别结果。
- [x] 从 25725 的 Beta 更新提示下载并覆盖安装 25745，确认订阅、节点和路由规则保留。
- [ ] 补测域名诊断、日志搜索／高亮、清除日志后采集新连接，以及订阅刷新。
- [x] 构建 1.0.3 稳定候选 25770。CI 核对与 25745 相同的 applicationId 和签名证书，且 versionCode 高于 25745。
- [x] 实机验证 25745 → 25770 覆盖安装、配置保留与更新渠道。
- [x] 实机验收 25780 的繁中更新频道文案（「穩定版」和「Beta 版」）。
- [x] 构建显示 `1.0.3-STABLE` 的签名候选 25800。
- [x] 实机验证 25780 → 25800 覆盖安装、配置保留和 Android 系统版本显示。
- [x] 构建 25810 Release 候选，验证包名、签名、版本号及 APK 未开启调试。
- [x] 验证 25800 → 25810 覆盖安装。「关于」显示 `1.0.3-STABLE`、构建号 25810，无 `DEBUG`，配置保留，VPN 正常连接。
- [x] 将同一份已验收 APK 发布为 GitHub 最新稳定版。
- [x] 发布后实测：25810 的稳定版和 Beta 版更新频道均显示已安装 25810、可用 25810，无需更新。

## 8. GitHub Actions 与 Release 状态

| 工作流程 | 结果 | 发布 |
| --- | --- | --- |
| [1.0.3 正式发布](https://github.com/LauritzWu/Veyr/actions/runs/44576152224) | 发布包与 CI APK 的 SHA-256 一致；已验证为最新稳定版 | [v1.0.3-stable.25810](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.3-stable.25810) |
| [CI #62](https://github.com/LauritzWu/Veyr/actions/runs/44573687065) | 成功。1.0.3-STABLE（25810），签名、包名、版本及非调试属性校验通过 | [Release 候选产物](https://github.com/LauritzWu/Veyr/actions/runs/44573687065/artifacts/14555717962) |
| [CI #60](https://github.com/LauritzWu/Veyr/actions/runs/44570837995) | 成功。1.0.3-STABLE（25800），ARM64 签名、包名和构建号校验通过 | [稳定候选产物](https://github.com/LauritzWu/Veyr/actions/runs/44570837995/artifacts/14555320309) |
| [CI #56](https://github.com/LauritzWu/Veyr/actions/runs/44566873866) | 成功。1.0.3（25780），ARM64 签名与对齐校验通过 | [文案更新候选产物](https://github.com/LauritzWu/Veyr/actions/runs/44566873866/artifacts/14552262272) |
| [CI #54](https://github.com/LauritzWu/Veyr/actions/runs/44563328107) | 成功。1.0.3（25770），包名、构建号、ARM64 签名与对齐校验通过 | [稳定候选产物](https://github.com/LauritzWu/Veyr/actions/runs/44563328107/artifacts/14551053416)，已通过实机覆盖安装 |
| [CI #49](https://github.com/LauritzWu/Veyr/actions/runs/44559517827) | 成功。1.0.3-beta.3（25745），通过 ARM64 签名与对齐校验 | [候选产物](https://github.com/LauritzWu/Veyr/actions/runs/44559517827/artifacts/14548422195) |
| [Beta.3 发布](https://github.com/LauritzWu/Veyr/actions/runs/44560975120) | 成功。发布 APK 的 SHA-256 与候选 APK 一致 | [v1.0.3-beta.3.25745](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.3-beta.3.25745) |
| [CI #45](https://github.com/LauritzWu/Veyr/actions/runs/44556469063) | 成功。1.0.3-beta.2 DEBUG（25725），ARM64 签名与对齐验证通过 | [修复候选产物](https://github.com/LauritzWu/Veyr/actions/runs/44556469063/artifacts/14544419251)，已通过实机回归 |
| [CI #43](https://github.com/LauritzWu/Veyr/actions/runs/44546508392) | 成功。LibCore、OSS APK、ARM64 签名和对齐检查通过 | [原始候选产物](https://github.com/LauritzWu/Veyr/actions/runs/44546508392/artifacts/14540883341) |
| [Beta 发布](https://github.com/LauritzWu/Veyr/actions/runs/44554631626) | 成功。重新下载的 APK 与候选 APK 的 SHA-256 一致 | [v1.0.3-beta.2.25715](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.3-beta.2.25715) |
| [稳定版发布](https://github.com/LauritzWu/Veyr/actions/runs/44541336712) | 成功 | [v1.0.2-stable.25685](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.2-stable.25685) |

## 9. 最近开发记录及相关 Commit

| Commit | 内容 |
| --- | --- |
| [`83880d5`](https://github.com/LauritzWu/Veyr/commit/83880d519423cf9e2d3e439f741f3fa2124fcdb8) | 繁中更新频道缩短为「Beta 版」 |
| [`68de6ad`](https://github.com/LauritzWu/Veyr/commit/68de6adf9550314658e05335883a3a9a2ce81a8e) | 在 `release/1.0.3-candidate` 构建稳定候选，沿用已验收的 Beta.3 应用代码 |
| [`6720338`](https://github.com/LauritzWu/Veyr/commit/6720338fbf15473168d4d6255c0f4c6243df353f) | 配置 Beta.3 签名构建，通过同一版本变量生成校验值和产物名称 |
| [`396b98e`](https://github.com/LauritzWu/Veyr/commit/396b98ef2462ff1ce949dcc636486158857b79ed) | 修复 Beta 标签和对应 ARM64 APK 的筛选 |
| [`4158559`](https://github.com/LauritzWu/Veyr/commit/4158559d3a7a1a1e32d75bd2c5468649083a462e) | 在 release/beta-25715 增加预发布工作流程 |
| [`712445f`](https://github.com/LauritzWu/Veyr/commit/712445fa7f4ad8aae5ace0e2def9a605b644f0f7) | 配置 1.0.3-beta.2 签章构建 |
| [`bd480e1`](https://github.com/LauritzWu/Veyr/commit/bd480e1) | 更新路由与连接诊断的繁中资源 |
| [`4787083`](https://github.com/LauritzWu/Veyr/commit/4787083) | 添加按应用的连接诊断 |
| [`62731eb`](https://github.com/LauritzWu/Veyr/commit/62731eb177122d52a809ff2e72e8801aeef15921) | 默认分支恢复 README 致谢 |

## 10. 新 Work 对话交接与长期维护规则

1. 先读仓库默认分支根目录的 `DEVELOPMENT_STATUS.md`，再核对目标分支 HEAD、最近的 Actions，以及 GitHub 最新稳定版和预发布版。
2. 确认要修改的分支与 APK 的源码提交。已通过的应用连接诊断和覆盖安装用例作为回归基线。
3. 每次完成开发、修复 Bug、通过测试或发布版本时，同步更新本文。记录分支、Commit、Actions run、Release、versionCode、测试环境与结果。
4. 分开记录源码已实现、CI 验证、实机通过、失败及待验证。每项通过结果标明所测构建和证据，更新路线图与已知问题，保留相关历史链接。
5. 新 Work 对话可从这里开始：“请读取 LauritzWu/Veyr 默认分支的 DEVELOPMENT_STATUS.md，核对当前分支和发布状态，从最高优先级未完成项继续。”

## 发版自动化演练

- 独立分支 `ci/release-automation` 增加 Android 发布草稿工作流。它使用同一组输入生成版本显示名、发布标签和下载文件名。
- 用已发布的 25810 进行演练，校验 APK 包名、签名、ARM64、构建号、非调试属性及 SHA-256，并校验重新下载的草稿附件。最新稳定版仍为 25810。[演练工作流](https://github.com/LauritzWu/Veyr/actions/runs/44658007821)
- 依据应用更新规则和 GitHub Release 数据进行模拟：现有稳定和 Beta 渠道均选中 25810；加入模拟 Beta 25815 后，只有 Beta 渠道选中它。Android 更新器的 Kotlin 规则已接入 CI：[回归通过](https://github.com/LauritzWu/Veyr/actions/runs/44660823023)。覆盖草稿过滤、稳定／Beta 分流、ARM64 文件选择、历史预览格式及防降级。
- [x] 在独立分支让构建端与发版草稿复用版本、渠道和 Beta 轮次配置。
- [x] 将签名构建及草稿工作流改为手动触发。修改后普通提交仅运行更新回归，Beta 25820 继续保留为草稿。
- [x] 从默认分支单独移植更新规则、回归测试和手动发版工作流。[PR #1](https://github.com/LauritzWu/Veyr/pull/1) · [PR 回归通过](https://github.com/LauritzWu/Veyr/actions/runs/44661161037)
- [x] PR #1 的回归与完整 APK 构建通过，已合并至默认分支。GitHub Actions 页面已验证显示 Run workflow 入口。[手动工作流](https://github.com/LauritzWu/Veyr/actions/workflows/android-build-and-draft.yml)
- [x] 从默认分支手动构建 `1.0.4-STABLE`（25840）：ARM64 APK 签名、版本、非调试属性及草稿附件 SHA-256 校验通过。最新公开稳定版仍是 25810。[构建记录](https://github.com/LauritzWu/Veyr/actions/runs/44672030353) · [Release 草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-661a8ff32ecba6875b92)
- [x] S24 Ultra 实机通过 25835 → 25840 覆盖安装。「關於」显示 `1.0.4-STABLE`、組建編號 25840；繁中界面正常，订阅、节点和路由规则保留，VPN 可连通。
- [x] 发布已验收的 25840 为 GitHub 最新稳定版，保留 `veyr-preview-arm64-v8a.apk`，附件 SHA-256 与草稿一致。[发布页](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.4-stable.25840) · [发布校验](https://github.com/LauritzWu/Veyr/actions/runs/44673641147)
- [x] 一加 7T 实测升级到 `1.0.4-STABLE`（25840）。订阅、节点和路由规则均保留，VPN 可连通。
- [x] 一加 7T 实测从 25810 的「檢查更新 → 穩定版」发现 25840，并从更新提示下载 APK，完成覆盖安装。
- [x] 用真实签名 Beta（`1.0.4-BETA`，25820）验证从构建到草稿。APK 与草稿附件的 SHA-256 一致，最新公开稳定版仍为 25810。[工作流](https://github.com/LauritzWu/Veyr/actions/runs/44658581003)

## 下载入口与安装体验

- [x] README 提供最新稳定版 ARM64 APK 直链、当前公开 Beta APK 直链以及 Beta 发布列表。发布新 Beta 时同步更新 README 的固定版本直链。
- [x] 手动发版模板正文首行包含 ARM64 APK 和 SHA-256 下载链接。链接在 Release 正式发布后可用。
- [x] 应用内下载、校验包名／构建号／签名并打开 Android 安装确认页已在 [PR #2](https://github.com/LauritzWu/Veyr/pull/2) 实现。[完整 APK 构建](https://github.com/LauritzWu/Veyr/actions/runs/44663472610)与[频道回归](https://github.com/LauritzWu/Veyr/actions/runs/44663472469)通过。
- [x] 已从 PR #2 分支手动构建 `1.0.4-BETA`（25825，Beta.2）。签名验证和草稿附件哈希核对通过，草稿正文首行已生成 APK 与 SHA-256 链接。[构建记录](https://github.com/LauritzWu/Veyr/actions/runs/44664088544)
- [x] S24 Ultra 实机覆盖安装 25810 → 25825。显示 `1.0.4-BETA` 与构建号 25825，原有订阅、节点和路由规则均保留。
- [x] 已发布签名 Beta.3（25830）作为实测目标，与工作流草稿 APK 的 SHA-256 一致；稳定版仍为 25810。[预发布版](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.4-beta.3.25830)
- [x] S24 Ultra 实机通过 25825 → 25830 应用内下载与覆盖安装。订阅、节点和路由规则均保留。
- [x] S24 Ultra 实测通过 25830 → 25835；确认「關於」、更新頻道及連線診斷恢复繁体中文。[Beta.4](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.4-beta.4.25835)
- [x] PR #2 在签名构建、完整 APK 构建和更新频道回归通过后合并至默认分支。[合并提交](https://github.com/LauritzWu/Veyr/commit/ae2ddc435cfdb483a9019c2e992c8373caa58fbf)
- [x] [PR #4](https://github.com/LauritzWu/Veyr/pull/4) 实现版本化 APK 文件名：稳定版 `veyr-1.0.6-stable-arm64-v8a.apk`、Beta `veyr-1.0.6-beta.1-arm64-v8a.apk`。更新器优先使用与发布标签一致的新文件名，发布流程同时提供字节一致的旧名兼容附件并校验两者 SHA-256。
- [x] [文件名与更新频道回归](https://github.com/LauritzWu/Veyr/actions/runs/44687294618)和[完整 APK 构建](https://github.com/LauritzWu/Veyr/actions/runs/44687294684)通过。
- [x] 已公开过渡 Beta.1（25870），新旧文件名的签名 APK 逐字节一致，SHA-256 相同；最新稳定版仍为 25865。[预发布](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.6-beta.1.25870) · [构建](https://github.com/LauritzWu/Veyr/actions/runs/44688848843)
- [x] S24 Ultra 已安装 `1.0.6-BETA`（25870），订阅、节点和路由规则均保留。
- [x] S24 Ultra 从 25865 的「檢查更新 → Beta 版 → 下載並安裝」发现、下载并覆盖安装 25870。旧文件名兼容实测通过，订阅、节点和路由规则保留。
- [x] 已发布 Beta.2（25875），仅提供 `veyr-1.0.6-beta.2-arm64-v8a.apk` 和 SHA256SUMS，签名及草稿附件校验通过；稳定版仍为 25865。[预发布](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.6-beta.2.25875) · [构建](https://github.com/LauritzWu/Veyr/actions/runs/44690989074)
- [x] S24 Ultra 实测从 25870 的 Beta 频道下载并覆盖安装 25875。Beta.2 只提供版本化 APK，确认新版更新器能识别该文件名，且订阅、节点和路由规则保留。
- [x] 完整构建与更新频道回归通过，PR #4 已合并至默认分支。[合并提交](https://github.com/LauritzWu/Veyr/commit/3a2b2223dbe787a69abe72a3c2aa1c545d5c15a6)
- [x] 将构建 25870 时验证过的 GeoIP／GeoSite 查询修复移植到 PR #4 并随 PR 合并。[提交](https://github.com/LauritzWu/Veyr/commit/17f779add0509db8dbd04f62e8fd6b8385bb3bdb)

## 下一轮 Beta 更新体验

- [x] [PR #3](https://github.com/LauritzWu/Veyr/pull/3) 增加可取消的下载百分比和验证阶段提示，区分下载、校验和打开系统安装器失败，提供重试和发布页入口。
- [x] 关键繁中更新、诊断和规则集资源加入 CI，检查缺失、重复及占位符。[回归构建](https://github.com/LauritzWu/Veyr/actions/runs/44675481432)与[完整 APK 构建](https://github.com/LauritzWu/Veyr/actions/runs/44675481337)通过。
- [x] 通过独立发布分支从 PR #3 构建 `1.0.5-BETA`（25845、Beta.1），签名和草稿附件 SHA-256 校验通过。[构建](https://github.com/LauritzWu/Veyr/actions/runs/44676275188) · [草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-cc605e778be92410d787)
- [x] S24 Ultra 覆盖安装 `1.0.5-BETA`（25845）通过。「關於」显示正确版本和組建編號，订阅、节点和路由规则均保留。
- [x] 已发布签名 Beta.2（25850）作为 25845 的实测目标。校验草稿附件 SHA-256 后公开，最新稳定版仍为 25840。[预发布](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.5-beta.2.25850) · [构建](https://github.com/LauritzWu/Veyr/actions/runs/44678456851)
- [x] S24 Ultra 实测 25845 → 25850：实际下载失败显示重试；下载完成显示「正在驗證安裝套件」，随后打开 Android 安装确认页。覆盖安装后订阅、节点和路由规则保留。
- [x] 主动取消修复已通过 25855 实机复测：进度框直接关闭，不再提示下载失败。
- [x] 已构建签名 Beta.3（25855），包含主动取消修复；草稿附件 SHA-256 复核通过。[构建](https://github.com/LauritzWu/Veyr/actions/runs/44680499788) · [草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-a5f20abb37c549d317ba)
- [x] S24 Ultra 完成 25850 → 25855 覆盖安装。「關於」显示組建編號 25855，订阅、节点及路由规则保留。
- [x] 发布签名 Beta.4（25860）供 25855 复测，草稿附件 SHA-256 校验通过，最新稳定版仍为 25840。[预发布](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.5-beta.4.25860) · [构建](https://github.com/LauritzWu/Veyr/actions/runs/44682269796)
- [x] S24 Ultra 实测 25855 → 25860：主动取消静默返回；实际下载失败显示「無法下載安裝套件」及「重試」按钮；可打开 Android 安装确认页，升级后配置保留。
- [x] [PR #3](https://github.com/LauritzWu/Veyr/pull/3) 完整 APK 构建、更新频道与繁中资源回归通过，已合并至默认分支。[合并提交](https://github.com/LauritzWu/Veyr/commit/814d7a7b21a99e76073f23694c71ea630d4cab92)
- [x] 从默认分支提交 `ab15bff` 构建签名 `1.0.5-STABLE` 候选（25865）；包名、构建号、签名、非调试属性与草稿附件 SHA-256 检查通过。[构建](https://github.com/LauritzWu/Veyr/actions/runs/44683970252) · [草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-08d2e0d7e48180113ec1)
- [x] S24 Ultra 实测 25860 → 25865 覆盖安装通过。「關於」显示 `1.0.5-STABLE`、組建編號 25865，繁中正常，订阅、节点和路由规则保留，VPN 能连通。
- [x] 已验收的 25865 发布为 GitHub 最新稳定版，APK 文件名保留 `veyr-preview-arm64-v8a.apk`，SHA-256 与草稿一致。[发布页](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.5-stable.25865) · [发布校验](https://github.com/LauritzWu/Veyr/actions/runs/44685952066)
- [x] 一加 7T 升级到 `1.0.5-STABLE`（25865），订阅、节点和路由规则保留，VPN 可连通。
- [x] 一加 7T 实测从 25840 的「檢查更新 → 穩定版 → 下載並安裝」发现、下载并覆盖安装 25865，稳定频道升级全流程通过。
- [x] 从默认分支构建签名 `1.0.6-STABLE`（25880）；版本化 APK 与旧名兼容附件逐字节一致，SHA-256 相同；草稿尚未公开。[构建](https://github.com/LauritzWu/Veyr/actions/runs/44693058466) · [草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-af58e36d748a31b966e5)
- [x] S24 Ultra 实测 25875 → 25880 覆盖安装通过。「關於」显示 `1.0.6-STABLE`、組建編號 25880，繁中界面正常，订阅、节点和路由规则保留，VPN 可连通。
- [x] 已验收的 25880 发布为 GitHub 最新稳定版；主下载使用版本化文件名，保留旧名附件；两个 APK 的 SHA-256 一致。[发布页](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.6-stable.25880) · [发布校验](https://github.com/LauritzWu/Veyr/actions/runs/44695196146)
- [x] 一加 7T 从 25865 的「關於 → 檢查更新 → 穩定版 → 下載並安裝」升级到 25880。订阅、节点和路由规则保留，VPN 可连通；稳定频道兼容文件名实测通过。

## 单一 APK 发布

- [x] [PR #5](https://github.com/LauritzWu/Veyr/pull/5) 从新发布移除兼容附件，简化标题及首轮 Beta 文件名；保留历史下载识别。完整 APK 构建及频道回归通过。
- [x] 签名 `1.0.7-BETA`（25885）草稿仅包含一个 APK `veyr-1.0.7-beta-arm64-v8a.apk` 和 SHA256SUMS。[草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-8f037e7d2a36a34c4313) · [构建](https://github.com/LauritzWu/Veyr/actions/runs/44697571532)
- [x] S24 Ultra 实测从 25880 手动覆盖安装单 APK 草稿 25885。「關於」显示 `1.0.7-BETA`、組建編號 25885；订阅、节点和路由规则保留，VPN 可连通。
- [x] 发布单 APK `1.0.8-BETA`（25890），唯一 APK 为 `veyr-1.0.8-beta-arm64-v8a.apk`，签名及草稿附件 SHA-256 校验通过；最新稳定版仍为 25880。[预发布](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.8-beta.1.25890) · [构建](https://github.com/LauritzWu/Veyr/actions/runs/44699727714)
- [x] S24 Ultra 已安装 `1.0.8-BETA`（25890）。订阅、节点和路由规则保留，VPN 可连通。
- [x] S24 Ultra 通过 25885 的「檢查更新 → Beta 版 → 下載並安裝」发现并覆盖安装 25890；简化文件名的单 APK 升级路径实测通过。
- [x] PR #5 的完整构建和更新回归通过后已合并到默认分支，后续新发布只生成一份版本化 APK。[合并提交](https://github.com/LauritzWu/Veyr/commit/833623bdbeff447efe3426b6d136250c84bffec1)
- [x] 将 1.0.8-BETA（25890）推进至签名 `1.0.8-STABLE` 候选（25895）；草稿只有版本化 APK 和 SHA256SUMS，构建、签名与附件校验通过。[构建](https://github.com/LauritzWu/Veyr/actions/runs/44702438731) · [草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-b2c29d14b2a0d357543c)
- [x] S24 Ultra 实测从 25890 覆盖安装 25895。「關於」显示 `1.0.8-STABLE`、組建編號 25895，繁中界面正常，订阅、节点和路由规则保留，VPN 可连通。
- [x] 已验收的 25895 发布为 GitHub 最新稳定版，唯一 APK 为 `veyr-1.0.8-stable-arm64-v8a.apk`，SHA-256 与实测草稿一致。[发布页](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.8-stable.25895) · [发布校验](https://github.com/LauritzWu/Veyr/actions/runs/44704884061)
- [x] 一加 7T 已升级至 `1.0.8-STABLE`（25895），订阅、节点和路由规则保留，VPN 可连通。
- [x] 一加 7T 从 25880 的「檢查更新 → 穩定版 → 下載並安裝」发现、下载并覆盖安装 25895，单一版本化 APK 的稳定频道升级全流程通过。

## 日志保留与历史查看

- [x] [草稿 PR #6](https://github.com/LauritzWu/Veyr/pull/6)：默认缓冲区 256 KiB，保留已保存设置。实时窗口仍为 50 KiB；菜单可按需读取最多 256 KiB 的保留日志，查看历史时暂停自动刷新。
- [x] 完整 APK 构建通过；`1.0.9-BETA` 单 APK 签名草稿通过附件校验。[草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-be0c2402d12d5e24e220) · [签名构建](https://github.com/LauritzWu/Veyr/actions/runs/44709269474)
- [ ] 在 S24 Ultra 复测高流量下的长列表滚动流畅度。
- [x] S24 Ultra 截图确认 YouTube 关键词过滤和高亮可用。
- [ ] VPN 启动时 Wi-Fi 状态查询在 Android 缺少权限时抛出异常。已在 PR #6 加入权限检查；[修复提交](https://github.com/LauritzWu/Veyr/commit/c1e94e0b6055a8539546a99238dfd18cb69129ab)，待包含修复的新 APK 实测。
- [ ] 设备选择 Info 后仍看到核心 DEBUG／TRACE；普通生成配置映射为 info，原因已确认：核心 PlatformWriter 未按日志级别过滤。PR #6 的原生 writer 按实例有效级别过滤，待实机确认。
- [x] 两项修复已纳入签名 `1.0.9-BETA.2` 草稿，完整构建、签名及 SHA-256 校验通过。[草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-6cf247af4ca884ddfeea) · [构建](https://github.com/LauritzWu/Veyr/actions/runs/44714332034)
- [x] S24 Ultra 在 1.0.9-BETA.2 的 Info 测试截图中确认新核心输出为 INFO，未见 DEBUG／TRACE。截图可见 `[0058]` 至 `[0091]`，未确认是否为同一次历史快照。
- [x] S24 Ultra 复测 1.0.9-BETA.2：所提供的 VPN 启动日志中未见 `ACCESS_WIFI_STATE` 异常堆栈。启动时短暂提示 `missing default interface`，随后发现移动网络接口并继续处理 DNS 流量。
- [x] 用户确认从「查看保留的日誌」切回「最新」并播放后，新日志继续出现。
- [x] S24 Ultra 在同一轮 YouTube 播放测试中确认，`1.0.9-BETA.2` 的「最新」截图顶部为 `INFO[0012]`，保留视图可回看启动配置及 `INFO[0000]`，按核心相对时间计多回看约 12 秒。此结果为当次实测，实际时长随日志量变化。
- [x] 代码核对确认 `ProxyInstance` 在常规启动路径写入完整配置 JSON，`SendLog` 导出直接拼接 `logcat -d` 与完整 `neko.log`，未进行字段脱敏。当前实机截图仅确认配置 JSON 可见，未检查含有效凭据的导出样本。
- [x] PR #6 移除 Android 启动时的完整 sing-box 与插件配置输出；升级后首次初始化在 UI 和 VPN 进程之间加锁清理旧 `neko.log`。[配置日志提交](https://github.com/LauritzWu/Veyr/commit/12022a2a59a105bb33f72d72e31c5fb5f31b4999) · [缓存迁移提交](https://github.com/LauritzWu/Veyr/commit/ac0a17dff9f28dcf899a64e1fd843deffa412976)
- [x] 用户实测覆盖安装后，「關於」显示 `1.0.9-BETA.3`，订阅、节点和路由规则均保留。
- [x] `1.0.9-BETA.3` 用户反馈历史视图未见旧配置，应用内搜索未找到虚拟节点测试密码。核对完整导出文件，确认包含虚拟节点的连接拒绝记录，未找到测试密码或 `ProxyInstance` 完整配置输出。
- [x] `1.0.9-BETA.3` 实机确认：点击工具栏向下箭头回到最新视图并打开网页后，可见新增 INFO DNS 和出站连接日志。
- [x] PR #6 已合并至默认分支。[合并提交](https://github.com/LauritzWu/Veyr/commit/1153ec5b79574a7765698cce3c36c75cb601d410)
- [x] 签名构建 #8 来自 `feature/retained-log-history` 的 `ac0a17d`，ARM64 签名候选校验及草稿校验步骤通过。`1.0.9-BETA.3` 草稿（25915）包含 `veyr-1.0.9-beta.3-arm64-v8a.apk` 与 `SHA256SUMS`。[构建](https://github.com/LauritzWu/Veyr/actions/runs/44722542301) · [草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-c9a82aa972d24f2f8c1e)
- [x] `1.0.9-BETA.3`（25915）已发布为 Pre-release。发布页显示来源提交 `ac0a17d`、单一 ARM64 APK 与 `SHA256SUMS`。[发布页](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.9-beta.3.25915)
- [x] 发布后实机反馈：「關於」显示 `1.0.9-BETA.3`，订阅、节点和路由规则均保留，VPN 可连通。
- [x] 用户确认稳定频道未推荐 `1.0.9-BETA.3`。
- [x] 用户确认通过 Veyr 应用内 Beta 频道的「下載並安裝」完成 `1.0.9-BETA.3` 升级。版本显示正确，订阅、节点和路由规则保留，VPN 可连通。
- [x] `1.0.9-STABLE` 候选包实机验证通过：「關於」版本正确，订阅、节点和路由规则均保留，VPN 可连通，实时日志继续刷新。
- [x] 用户确认实测 APK 来自 `1.0.9-STABLE` 发布草稿（25920）；截图显示附件 `veyr-1.0.9-stable-arm64-v8a.apk` 与 `SHA256SUMS`。
- [x] 签名构建来自默认分支 `lauritz/starifly-core-1.14` 的 `1abc714`，构建号 25920，ARM64 签名候选验证与草稿验证通过。[构建](https://github.com/LauritzWu/Veyr/actions/runs/44732704088) · [草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-b80c2c23da21e85e3928)
- [x] `1.0.9-STABLE`（25920）已发布，用户确认旧版设备通过下载完成升级。[正式发布](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.9-stable.25920)
- [x] 用户确认旧版设备通过 Veyr 应用内「检查更新 → 稳定版 → 下载并安装」成功升级到 `1.0.9-STABLE`（25920），稳定频道应用内升级验证通过。

## 上游对照回归（待测）

对照对象为 Veyr `1.0.9-STABLE` 和 Starifly NekoBoxF。先在一加 7T 实测，记录两款客户端及核心版本。使用相同订阅、节点、网络、日志级别、视频和画质，每次只运行一款 VPN。

- [ ] 各播放同一段 YouTube 视频 2 分钟，检查打开日志页、滚动和回到最新记录是否流畅。
- [ ] 各刷新一次相同的 Sub-Store 订阅，记录节点列表及原有路由行为，再断开并重新连接，确认网页可访问。
- [ ] 如可通过 ADB 采样，记录 VPN 进程和界面进程的 PSS，并分别标注空闲、播放、打开日志页时的读数。

## Android 快速设置磁贴

- [ ] 验证独立的单色鹰头侧影、控制项列表的 Veyr 名称，以及连接和断开状态在 One UI 上的显示。磁贴沿用现有 VPN 状态和点击行为。
- [x] `1.0.10-BETA.1`（25925）在 One UI 的「新增控制項」显示 Veyr 和新图标；用户反馈鹰头辨识度不足。[构建](https://github.com/LauritzWu/Veyr/actions/runs/44881976688)
- [x] 用户确认采用 `1.0.10-BETA.3`（25935）的缩短鹰嘴版本作为快速设置磁贴图标。[签名构建](https://github.com/LauritzWu/Veyr/actions/runs/44885739057)
- [x] 用户在 One UI 实测 `1.0.10-BETA.3`（25935）：点击快速设置磁贴连接和断开 VPN 均正常。
- [x] `1.0.10-BETA.3`（25935）已发布为 GitHub Pre-release，使用 One UI 实测通过的签名 ARM64 APK。[发布页](https://github.com/LauritzWu/Veyr/releases/tag/v1.0.10-beta.3.25935)
- [x] `1.0.10-BETA.3` 发布后实机升级验证：用户确认「关于」显示版本正确，订阅、节点及路由规则均保留。此次反馈未单独确认下载入口及稳定频道筛选。
- [x] `1.0.10-STABLE`（25940）签名 ARM64 候选构建成功，已生成发布草稿。[构建](https://github.com/LauritzWu/Veyr/actions/runs/44900914021) · [草稿](https://github.com/LauritzWu/Veyr/releases/tag/untagged-560341b8072a660e6c0d)
- [x] 用户实测通过 `1.0.10-STABLE`（25940）：关于页版本和构建号正确，覆盖安装保留订阅、节点和路由规则，One UI 显示缩嘴鹰头且磁贴可正常连接和断开 VPN。
- [x] 正式发布 `1.0.10-STABLE`（25940），GitHub 标记为 Latest。
- [ ] 验证稳定频道应用内升级至 `1.0.10-STABLE`（25940）。

### 訂閱刷新回歸

使用 [測試訂閱伺服器](buildScript/test-subscription-refresh.py) 在同一條 URL 切換回應。範例節點只用於匯入與刷新測試，實際連線另用可用的 Sub-Store 節點驗證。

1. 在電腦執行 `Set-Content scenario.txt baseline`，再執行 `python buildScript/test-subscription-refresh.py --scenario-file scenario.txt`。
2. 以 USB 連接測試手機，執行 `adb reverse tcp:8765 tcp:8765`。在 Veyr 建立獨立的測試訂閱群組，地址填 `http://127.0.0.1:8765/subscription`。測試時保持 VPN 斷線，按提示確認刷新本機 HTTP 訂閱。
3. 從訂閱分頁工具列刷新。切換回應時，在另一個終端機執行 `Set-Content scenario.txt <情境>`。測試完畢後執行 `adb reverse --remove tcp:8765` 並停止測試伺服器。

| 情境 | 實機驗收 |
| --- | --- |
| `baseline` | 匯入 VLESS 和 Hysteria2。選取 Fixture-VLESS，建立一條指向它的測試路由。切到手動群組時刷新入口隱藏。 |
| `rename` | 摘要顯示變更 1、新增 0、移除 0。VLESS 改名後仍是所選節點，既有測試路由仍指向該節點。 |
| `unavailable` | 同一 URL 回應 HTTP 503。顯示下載失敗，保留節點清單、選取狀態、路由與上次成功時間。 |
| `invalid` | 同一 URL 回應 HTTP 200，內容沒有可用節點。顯示內容錯誤，保留與前一項相同的狀態。 |
| `remove` | 先取消確認，兩個節點與上次成功時間仍保留。再次刷新並確認後只移除 VLESS，摘要顯示移除 1，並提示原選取節點被移除。 |

已用本機 HTTP 請求驗證五種回應及改名前後的連線參數。Veyr 實機測試待執行。
