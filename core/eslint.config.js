// @ts-check
import tseslint from 'typescript-eslint'
import globals from 'globals'

/**
 * DepMap core ESLint（flat config）。
 * 规则原则（RC_AUDIT_RULES）：critical path（impact/crypto/parser/fingerprint/proposal/
 * repository/migration）最高标准；禁止 silent catch / floating promise /
 * unsafe any；禁止文件级全局关闭（无生成代码，故无文件级豁免）。
 */
export default tseslint.config(
  {
    ignores: ['node_modules', 'coverage', 'dist', 'tests/fixtures/**', 'eslint.config.js'],
  },
  ...tseslint.configs.recommendedTypeChecked,
  {
    files: ['**/*.ts'],
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
      globals: { ...globals.node },
    },
    rules: {
      // ---- 类型安全（RC PHASE D/F） ----
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-unsafe-assignment': 'error',
      '@typescript-eslint/no-unsafe-call': 'error',
      '@typescript-eslint/no-unsafe-member-access': 'error',
      '@typescript-eslint/no-unsafe-return': 'error',
      '@typescript-eslint/no-unsafe-argument': 'error',
      '@typescript-eslint/no-non-null-assertion': 'error',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      // ---- 错误处理（fail closed） ----
      'no-empty': ['error', { allowEmptyCatch: false }],
      '@typescript-eslint/no-floating-promises': 'error',
      '@typescript-eslint/no-misused-promises': 'error',
      '@typescript-eslint/await-thenable': 'error',
      'prefer-promise-reject-errors': 'error',
      // ---- 一致性 ----
      'no-console': 'error',
      'no-fallthrough': 'error',
      'no-constant-condition': 'error',
      eqeqeq: ['error', 'smart'],
      'prefer-const': 'error',
      'no-var': 'error',
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      '@typescript-eslint/no-unused-expressions': 'error',
    },
  },
  {
    // 测试代码：允许 vitest 全局与非空断言（断言场景合理）
    files: ['tests/**/*.ts', 'scripts/**/*.ts'],
    rules: {
      '@typescript-eslint/no-non-null-assertion': 'off',
      '@typescript-eslint/no-unsafe-assignment': 'off',
      '@typescript-eslint/no-unsafe-member-access': 'off',
      '@typescript-eslint/no-unsafe-argument': 'off',
      '@typescript-eslint/no-unsafe-call': 'off',
      'no-console': 'off',
    },
  },
  {
    // scripts 里的 CLI 工具允许 console 输出（唯一例外，见 LOGGING_AUDIT.md）
    files: ['scripts/**/*.ts'],
    rules: {
      'no-console': 'off',
    },
  },
)
