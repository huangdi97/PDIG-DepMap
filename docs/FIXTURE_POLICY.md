# FIXTURE_POLICY.md — 测试 Fixture 治理（Engineering Baseline V1）

## 分类

| 类别 | 位置 | 规则 |
|---|---|---|
| synthetic fixtures | `core/tests/fixtures/`（29 文件） | 必须可公开、不得含真实个人信息、命名自描述 |
| real-private | **禁止入库** | 真实账单只放 `local_private/`（gitignore），仅 `validate-real-bill.ts` 本地消费 |

## 命名

- 自描述场景名：`csv-eu-bank-semicolon.csv`、`ofx-missing-fitid.ofx`、`qfx-duplicate-fitid.ofx`。
- 禁止 `data2` / `tmp` / `test1` / `final` 式命名。
- 场景矩阵：来源（wechat/csv/ofx）× 编码（utf-8/BOM/GB18030）× 换行（LF/CRLF/CR-only）× 语义（退款/重复/坏行/缺列）。

## 特殊编码保护（防 formatter 破坏）

1. `.gitattributes`：`core/tests/fixtures/* -text`（禁止换行归一化/转码）。
2. `.editorconfig`：fixtures 段落 `charset/end_of_line/insert_final_newline/trim_trailing_whitespace` 全 unset。
3. prettier：fixtures 不在格式化范围（`tests/fixtures/**` 无源码扩展名文件）。
4. GB18030 / CR-only / BOM fixture 的行为断言在 `tests/parser/wechat.test.ts`、
   `tests/sources/generic-csv.test.ts` 中锚定（转码或换行改写会直接红）。

## 新增 fixture 规则

1. 先写失败测试，再提交 fixture（test-first）。
2. 内容必须 synthetic；人名/卡号/机构名使用明显虚构值。
3. 与现有 fixture 语义重复的不新增（防止 fixture 膨胀）；生成式大批量数据用
   `core/scripts/generate-fixtures.ts` 模式（代码生成 > 手工文件）。
