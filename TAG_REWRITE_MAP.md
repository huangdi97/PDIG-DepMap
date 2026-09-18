# TAG_REWRITE_MAP

Git 历史净化（git-filter-repo 字面量替换）导致的 ref / commit 重映射对照。

- 生成依据：`.git/filter-repo/ref-map`、`filter-repo/commit-map`
- 净化只做**字面量替换**：非生产测试签名口令 → `"<REDACTED_NONPROD_TEST_SECRET>"`；
  机器绝对路径 → `%USERPROFILE%` / `<repo>` / `<repo_parent>` / `<DEVECO_HOME>` 等占位符。
- 不删除提交、不改提交结构与提交信息；标注标签的 tagger / date / message 保持原值。

## Ref 映射与语义校验

| ref | 旧 object | 新 object | 类型 | 语义校验 | 说明 |
|---|---|---|---|---|---|
| `refs/heads/engineering/baseline-v1` | `def93896500b97f5c718a46946cef6d9e37a564a` | `b89ad8f3caa3faf8b9a630c1ef6fe10300f9bd18` | commit→commit | PRESERVED | author/committer/message 一致 · tree CHANGED(d7483a6d→6c5036d9) · subject=docs(status): WORK_STATUS — Engineering  |
| `refs/heads/feat/mvp02-global-source` | `482e545db17c1f2fa9101dd1fb62104d3f51855f` | `2ad62ca7b46247c4e2b91bf506340ccda141c9a0` | commit→commit | PRESERVED | author/committer/message 一致 · tree CHANGED(be8740c8→e42573d2) · subject=memory: record 2026-09-12 MVP02 handoff  |
| `refs/heads/feat/mvp03-living-graph` | `5a27a7c75881201de9b2da18607a52e8431802a6` | `ef035c6826bd5c1e32a81fc5d09643caf8e285f9` | commit→commit | PRESERVED | author/committer/message 一致 · tree CHANGED(afa1966f→3910e1fd) · subject=chore(sanitize): remove machine-absolute |
| `refs/heads/main` | `c8d5111c41f528fbbd46fd25956654365e8203b4` | `df9e14ba7a2de176fc932817b116bfe2bde53d8f` | commit→commit | PRESERVED | author/committer/message 一致 · tree CHANGED(bfc5b44f→c31099f4) · subject=chore(github): pre-publication audits, R |
| `refs/heads/master` | `75e2725e57fac052bdad984fb266848f3769048f` | `ac511f6d1899747119666d3bb12a5b6a21b1b75b` | commit→commit | PRESERVED | author/committer/message 一致 · tree CHANGED(c7126315→ff9167ef) · subject=docs: sync test counts (166) across FINA |
| `refs/tags/v0.2.0-mvp02` | `1d0d1f6fb01137e7322e6d2e99d1b768a46d7e10` | `9e28aa1504ec693c3f8208e67cfd2f07b6cf76ea` | commit→commit | PRESERVED | author/committer/message 一致 · tree CHANGED(921e57fa→3b4980d3) · subject=chore(engineering): track .codebuddy Wor |
| `refs/tags/v0.3.0-mvp03` | `21945e6620a6dcea2d6dac48163a42118d3af7b1` | `714d52ee47d50bf7c0687e2c7fd16f7c99ef8847` | commit→commit | PRESERVED | author/committer/message 一致 · tree CHANGED(30be7d6b→764febcc) · subject=docs(mvp03): freeze acceptance all 80 ga |
| `refs/tags/v0.3.0-uniapp-reference` | `8ff65fb31f96741a290d7426157ca64f756b99d7` | `f5f61f93d1a6633592f48b48d3d1fd432c46def3` | tag→tag | PRESERVED | tag=v0.3.0-uniapp-reference · tagger=huangdi97 <304418554@qq.com> 1789470310 +0800 · type=commit · target 6d268c0c27d7 → 7bc0ed323ea8 |

## Legacy 行为 Oracle 标签

- `v0.3.0-uniapp-reference` 已重指向净化后的等价提交：
  - 旧 tag object：`8ff65fb31f96741a290d7426157ca64f756b99d7`
  - 新 tag object：`f5f61f93d1a6633592f48b48d3d1fd432c46def3`，指向 commit `7bc0ed323ea82ce98139acd14eabd040a1ea111e`
  - tagger：`huangdi97 <304418554@qq.com> 1789470310 +0800`（未变）
  - message：`PDIG legacy uni-app x reference freeze (REFERENCE IMPLEMENTATION / BEHAVIOR ORACLE)`（未变）
  - `CONFORMANCE_MANIFEST.json` 中 `oracle.commit` 已同步为 `7bc0ed323ea82ce98139acd14eabd040a1ea111e`，与标签目标一致。

## 一致性结论

- 语义保全：全部 PRESERVED
- 旧对象因未执行 `git gc` 仍保留在本地（恢复源），但已不可达，不会进入推送范围。
