// Stryker 配置 — Engineering Baseline V1 变异测试 baseline（一次性，--no-save 安装）。
// 范围限定：Core critical modules 中语义密度最高的两个（Impact Kernel + RelationRegistry）。
// Proposal/Fingerprint 的语义防护由 contract/invariant 套件的人工变异验证补足
// （见 docs/MUTATION_TEST_REPORT.md）。
// 运行：npx stryker run
export default {
  packageManager: 'npm',
  testRunner: 'vitest',
  concurrency: 2,
  coverageAnalysis: 'perTest',
  mutate: ['src/impact/kernel.ts', 'src/domain/relation-registry.ts'],
  timeoutMS: 10000,
  timeoutFactor: 1.5,
  reporters: ['clear-text', 'html', 'json'],
  incremental: false,
  disableTypeChecks: false,
}
