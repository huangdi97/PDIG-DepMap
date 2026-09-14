# TODO_AUDIT.md — TODO/FIXME 审计（RC PHASE AC）

> 扫描：`grep -rn -iE "TODO|FIXME|HACK\b|XXX|PLACEHOLDER|not implemented|TEMP\b"
--include=*.ts/uts/uvue/kt/swift/ets/mjs core app platforms docs`

## 结果：release-blocking TODO = 0

命中全部为误报（API/代码标识符，非任务标记）：

| 命中                              | 性质                                  |
| --------------------------------- | ------------------------------------- |
| `mkdtemp()`（tests/perf ×3）      | Node.js `fs.mkdtempSync` 封装函数名   |
| `placeholders`（graph-serialize） | SQL 参数占位符变量名                  |
| `.uvue` 中 `placeholder=`（×2）   | HTML input 提示属性（前次扫描已排除） |

- 代码内无 TODO / FIXME / HACK / XXX / “not implemented” 注释。
- future 事项已集中于 `FUTURE.md`（MVP02+ backlog），不阻塞发布。
- 已知未完成项全部为外部工具链 Blocker（BLOCKERS.md B1–B3/B10），非代码内标记。
