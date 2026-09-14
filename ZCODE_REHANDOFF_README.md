# ZCODE_REHANDOFF_README.md

## 用途

WorkBuddy 没额度以后切回 ZCode 的接力包。不是新的 MVP02 启动包。

## 复制

把本包 4 个文件复制到当前已有项目根目录。
不要新建仓库。

不要覆盖：
AGENTS.md
CANONICAL_DESIGN.md
GOAL_MVP02_GLOBAL_SOURCE.md
MVP02_ACCEPTANCE.md
WORK_STATUS.md
BLOCKERS.md
CODEBUDDY.md
WorkBuddy 已修改源码

## ZCode 第一次

推荐新建一个 ZCode Task。

如果支持 `/goal`，Goal 设置为：

> 从当前真实仓库状态继续完成 MVP02。严格执行 ZCODE_MVP02_REHANDOFF_GOAL.md；先读取 WorkBuddy/CODEBUDDY/WORK_STATUS/Git diff，保护全部未提交修改，生成 ZCODE_REHANDOFF_AUDIT.md，从 MVP02_ACCEPTANCE 第一个未完成 Gate 接着执行。禁止从头重做、禁止 reset/clean、禁止进入 NEXT_BACKLOG。持续执行直到 MVP02_FINAL_REPORT.md；Real Data 保持 NOT_RUN。

然后发送：
ZCODE_MVP02_REHANDOFF_FIRST_PROMPT.txt

## 中途又断

新开 Task，发送：
ZCODE_MVP02_REHANDOFF_CONTINUE.txt

## 最终完成标志

根目录存在 MVP02_FINAL_REPORT.md，且真实：
MVP02_GLOBAL_SOURCE_ABSTRACTION = PASS

不能以“基本完成”代替 PASS。
