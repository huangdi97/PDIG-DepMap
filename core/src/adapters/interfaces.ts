/**
 * 统一平台 Adapter 接口 (AGENTS §6 / CANONICAL_DESIGN §8.2)
 *
 * 业务核心只依赖这些接口；平台差异通过 UTS 插件 + 原生实现隔离：
 * - Android: Kotlin（SQLCipher / Android Keystore / BiometricPrompt / FLAG_SECURE）
 * - iOS:     Swift（SQLCipher / Keychain / LocalAuthentication / 后台遮罩）
 * - HarmonyOS: ArkTS（ArkData relationalStore 加密 / HUKS / 官方用户认证）
 *
 * 三端逻辑 Schema 必须一致（schema/migrations.ts 的 DDL），数据库引擎可以不同。
 * 本文件为共享 Core 内的 TypeScript 契约；原生实现位于 uni_modules/depmap-* 与
 * platforms/ 目录，需在对应平台工具链编译验证（状态见 WORK_STATUS.md）。
 */

/** 打开参数：数据库文件名（不含路径）。密钥由 SecureKeyAdapter 提供。 */
export interface SecureDatabaseOpenOptions {
  dbName: string
}

/**
 * 加密数据库适配器。
 * 语义要求（三端一致）：
 * - open() 使用平台安全密钥解锁/创建数据库；
 * - 密钥错误或用户取消认证时 open() 必须失败，绝不降级为明文；
 * - migrate() 执行 SCHEMA_V1 迁移（幂等、事务化）；
 * - transaction() 失败回滚，不留半写入状态。
 */
export interface SecureDatabaseAdapter {
  open(options: SecureDatabaseOpenOptions): Promise<void>
  close(): Promise<void>
  migrate(): Promise<number>
  /** 执行只读查询（SQL 与参数由 Repository 层下发，与 Core DDL 一致）。 */
  query(sql: string, params: unknown[]): Promise<Record<string, unknown>[]>
  execute(sql: string, params: unknown[]): Promise<number>
  transaction<T>(fn: () => Promise<T>): Promise<T>
}

/** 平台安全密钥适配器：数据库密钥 / fpSecret 的存取。 */
export interface SecureKeyAdapter {
  /** 读取或首次生成数据库密钥（Android Keystore / iOS Keychain / HUKS）。 */
  getOrCreateDatabaseKey(alias: string): Promise<Uint8Array>
  /** 读取或首次生成 fpSecret（HMAC 指纹密钥）。 */
  getOrCreateFpSecret(): Promise<string>
  /** 密钥是否存在于安全存储。 */
  hasKey(alias: string): Promise<boolean>
  /** 清除密钥（用户主动删除数据时）。 */
  deleteKey(alias: string): Promise<void>
}

export type BiometricGateResult =
  | { ok: true }
  | { ok: false; reason: 'user_cancel' | 'lockout' | 'no_enrollment' | 'not_available' | 'error' }

/** 启动锁：系统生物识别 / 设备凭证。 */
export interface BiometricAdapter {
  /** 是否可用了生物识别/设备凭据（已注册等）。 */
  canAuthenticate(): Promise<boolean>
  /** 弹出系统认证；取消/失败不得进入数据层。 */
  authenticate(reason: string): Promise<BiometricGateResult>
}

/** 文件加解密（.depmap 导入导出）。平台实现 Argon2id + AES-256-GCM（同 V1 协议）。 */
export interface FileCryptoAdapter {
  /** 创建 .depmap 容器（与 Core golden vector 互操作）。 */
  encryptToDepmap(plaintext: Uint8Array, password: string): Promise<string>
  /** 打开 .depmap 容器；口令错/篡改抛错。 */
  decryptDepmap(json: string, password: string): Promise<Uint8Array>
}

/** 隐私屏（截屏保护）。 */
export interface PrivacyScreenAdapter {
  /** Android FLAG_SECURE / iOS 后台遮罩 / HarmonyOS 隐私窗口。 */
  setPrivacyScreen(enabled: boolean): Promise<void>
}
