# START_MVP03_FINAL_FREEZE.md

把本包文件复制到当前项目根目录。

如果 ZCode 支持 /goal，Goal 填：

完整执行 GOAL_MVP03_FINAL_FREEZE_RC_AUDIT.md。禁止进入 MVP04。先做 MVP03_FREEZE_PRE_AUDIT，最高优先级复核 PlanReadiness 是否存在“目标数减 Action 数”的 false-ready 风险，然后完成 graphRevision 原子性、Rebase、Drift、Candidate、ScenarioTemplate、Timeline、Verification、Schema v3/depmap、property/invariant/mutation、稳定性、安全隐私、MVP01/MVP02 回归以及 npm run check/check:full。Real Data 保持 NOT_RUN。直到生成 MVP03_FREEZE_REPORT.md 且真实给出 MVP03_FINAL_FREEZE = PASS。

然后发送：
ZCODE_MVP03_FREEZE_FIRST_PROMPT.txt

中途断了就发送：
ZCODE_MVP03_FREEZE_CONTINUE.txt
