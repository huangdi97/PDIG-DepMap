# WORKBUDDY_SETUP.md

## 1. 打开现有项目

用 WorkBuddy/CodeBuddy 打开当前 PDIG 项目根目录。
不要复制新仓库，不要重新初始化。

## 2. 模型与模式

模型选：DeepSeek-V4-Flash
工作模式选：Agent
不要选 Ask。
Plan 模式会先等确认，不适合这次连续执行。

## 3. 合并本包

复制本包到现有仓库根目录。
不要覆盖：
AGENTS.md
CANONICAL_DESIGN.md
GOAL_MVP02_GLOBAL_SOURCE.md
MVP02_ACCEPTANCE.md
WORK_STATUS.md
BLOCKERS.md
ZCode 已改源码

## 4. 第一次

新建一个 WorkBuddy Agent 任务，粘贴：
WORKBUDDY_FIRST_PROMPT.txt

## 5. 一次没跑完

新开任务，粘贴：
WORKBUDDY_CONTINUE_PROMPT.txt

它会从 WORK_STATUS 的 Next 继续。

## 6. 完成标志

必须存在 MVP02_FINAL_REPORT.md，并真实写：
MVP02_GLOBAL_SOURCE_ABSTRACTION = PASS

Real Data 可以保持 NOT_RUN。
平台 toolchain blocker 可继续存在。
