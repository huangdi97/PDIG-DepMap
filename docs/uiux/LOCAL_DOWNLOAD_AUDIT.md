# LOCAL_DOWNLOAD_AUDIT.md

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · 本轮一切网络获取的记录（spec §10、§58、§62、§90）

## 总览

| #   | 项                                                                         | 必要性                                                                                                                     | 来源                                                               | 版本                         | 网络大小 | 状态                                                          |
| --- | -------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ | ---------------------------- | -------- | ------------------------------------------------------------- |
| 1   | Gradle 8.9 distribution                                                    | 项目锁定依赖还原（spec §62「project dependency restore」：necessary / project-scoped / version locked；本机无全局 gradle） | services.gradle.org → GitHub release 307 重定向                    | 8.9-bin                      | ≈130 MB  | DOWNLOADED（经本地代理 127.0.0.1:10808；JVM `_JAVA_OPTIONS`） |
| 2   | Gradle/Maven 声明依赖（core/conformance/app + Compose Desktop 插件与依赖） | 项目声明依赖还原                                                                                                           | mavenCentral / google / gradlePluginPortal（均项目声明、版本锁定） | 各 build 文件锁定版本        | —        | RESTORED（`gradlew :app:jar` 与 android 构建所需）            |
| 3   | Android SDK system-image / AVD                                             | 不需要（已有 API36 `main` AVD）                                                                                            | —                                                                  | —                            | 0        | N/A                                                           |
| 4   | Skill（ui-ux-pro-max / impeccable / frontend-design）                      | 已装，不重装不更新（spec §9）                                                                                              | 本机 `~/.agents/skills/`                                           | 见 UIUX_SKILL_USAGE_AUDIT.md | 0        | N/A                                                           |
| 5   | 字体 / 图标 / stock art / AI 装饰包                                        | 禁止（spec §63、§42：noRemoteFonts/SVG/native icons）                                                                      | —                                                                  | —                            | 0        | 未下载                                                        |
| 6   | iOS 工具链                                                                 | 禁止（spec §60：Windows 宿主不下载 macOS VM / 黑苹果 / 第三方 remote Mac）；CI 用仓库既有 workflow                         | —                                                                  | —                            | 0        | N/A                                                           |
| 7   | Harmony DevEco / 模拟器镜像                                                | 外部门禁（BLOCKERS E-9：需华为账号 + DevEco 登录）。本机无 DevEco；**未尝试反复下载随机镜像**（spec §59）                  | —                                                                  | —                            | 0        | EXTERNAL_GATE                                                 |

**UNNECESSARY_DOWNLOAD_COUNT = 0**（唯一下载 #1 为项目锁定依赖还原，属 spec §62 允许类别，且逐项记录）。

## #1 明细

- **原因**：`desktop/settings.gradle.kts` 与 `android/gradle/wrapper/gradle-wrapper.properties` 锁定 `gradle-8.9-bin.zip`；本机 `where gradle` 无结果（无全局 Gradle），wrapper dists 缓存为空，首次构建必须还原。
- **来源**：`https://services.gradle.org/distributions/gradle-8.9-bin.zip`（307 → `github.com/gradle/gradle-distributions/...`）。
- **网络路径**：本机直连 github.com:443 失败；git 全局代理 `http://127.0.0.1:10808`。Gradle wrapper JVM 通过运行时 `_JAVA_OPTIONS=-Dhttp.proxyHost/-Dhttp.proxyPort/-Dhttps.proxyHost/-Dhttps.proxyPort` 走同一代理（成功：`gradlew --version` 显示 Gradle 8.9）。
- **校验**：wrapper 校验分发完整性后解压；`android/.gradle/8.9` + `%USERPROFILE%\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\` 就位；desktop `BUILD SUCCESSFUL`、android 构建链可跑。
- **写入位置**：仅 Gradle user home（`%USERPROFILE%\.gradle\...`）与项目 ASCII 构建根（`%USERPROFILE%\pdig-build` / `pdig-desktop-build`）—— 均为 toolchain runtime 状态，非项目源文件。

## 否定记录

- 未执行：`npm install -g`、`winget`、`choco`、`brew`、`curl | bash`、Android Studio 重装、sdkmanager 新镜像、avdmanager 新建、字体下载、素材下载。
- GitHub Actions：仅用于「现有 CI」按既有 workflow 运行（spec §61），不把 CI 当远程开发机。
