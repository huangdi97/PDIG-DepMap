# tools/harmony/forensics —— 宿主环境取证脚本

本目录保存**一次性但结论重要**的环境探测脚本。它们不是构建或门禁的一部分，
不进 CI，不需要维护；保留的原因只有一个：**结论不可从代码本身推出来**，
若将来在另一台机器上遇到同类现象，这些脚本能直接复用。

## 记录在案的宿主缺陷：OHOS clang 崩溃（`0xE06D7363`）

### 现象

调用 OHOS NDK 的 `clang` 交叉编译时，进程以 `0xE06D7363` 崩溃。
崩溃与**被编译的源码无关** —— 编译一个空的 `hello.c` 同样崩溃。

### 定位过程（脚本一一对应）

| 脚本 | 回答的问题 |
| --- | --- |
| `probe-clang-stage.py` | 崩溃发生在哪个阶段？（`-E` 正常 → 崩在 codegen/优化） |
| `probe-clang-pathfind.py` | 本机是否有可用的替代交叉编译路径？ |
| `probe-clang-parent-chain.py` | 哪种父进程链能成功调用 clang？ |
| `probe-clang-envdiff.py` | 「PowerShell 直调」与「Python 子进程调」的环境差在哪？ |
| `probe-clang-envbisect.py` / `probe-clang-envbisect2.py` | 二分：**哪个**环境变量导致崩溃？ |
| `probe-clang-envnarrow.py` | 「净化环境 + 保留必要变量」能否稳定编译？ |
| `probe-clang-opt-levels.py` | 崩溃是否与 `-O` 级别相关？ |
| `probe-clang-minrepro.py` | 剥掉 `--target` / `--sysroot` 后是否仍崩？ |
| `probe-clang-node-spawn.mjs` | node 侧复现（结果写文件，避开宿主管道） |

### 结论

**触发因子是环境变量 `SystemRoot`。**

| 子进程环境 | 结果 |
| --- | --- |
| `env = {}` | 编译成功 |
| `env = {PATH}` | 编译成功 |
| `env = {TEMP}` | 编译成功 |
| **`env = {SystemRoot}`** | **崩溃 `0xE06D7363`** |
| 全量继承 | 崩溃 |

根因判断：`SystemRoot` 使注入到子进程的
`MToolBox_v2.0.2\x64\MToolExtend.dll` 走入崩溃路径。
这是**宿主环境缺陷**，与本仓库代码无关。

**修复方式**（已落地在 `tools/harmony/ohos-clang-build.py`）：
`child_env()` 在构造子进程环境时**刻意省略 `SystemRoot`**。

> ⚠ 不要在 `ohos-clang-build.py` 里「顺手补全环境变量」。
> 把 `SystemRoot` 加回去会让整个原生构建重新崩溃，且崩溃信息（`0xE06D7363`）
> 完全看不出与 `SystemRoot` 有关。代码里已有注释说明，这里是第二道记录。
