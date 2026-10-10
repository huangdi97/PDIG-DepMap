# PDIG Recovery Preparedness UX Contract

> Date: 2026-10-10
> Status: **DESIGN_FROZEN / UI HIDDEN UNTIL FACTOR CANONICAL**
>
> Recovery Preparedness is a future child workspace under “我”.
> It is not Recovery Incident Mode and not a sixth primary destination.

## 1. Product question

Recovery Preparedness answers:

> **“Before anything goes wrong, which recovery/access resources have I actually
> established, which ones share the same failure domain, and what still needs review?”**

It does not answer:

> “Something is already lost — how do I recover right now?”

That is Recovery Incident.

## 2. Information architecture

Five primary destinations remain:

~~~text
现在 / 基础设施 / 变更 / 记录 / 我
~~~

Future hierarchy:

~~~text
我
→ 恢复准备
~~~

Possible contextual entry:
- “我” continuity section;
- Account/Service/Device Impact Lens;
- Weaknesses finding.

No primary tab is added.

## 3. Visibility gate

Do not render this screen until:
- AccessFactor / RecoveryFactor Canonical support exists;
- FactorBinding semantics exist;
- FailureDomain mapping can consume carriers;
- recovery uniqueness/path independence remains evidence-backed;
- production read model can project the same semantics.

Until then:

~~~text
RECOVERY_PREPAREDNESS_VISIBLE = false
~~~

A design mock/spec is not authority to expose the route.

## 4. Page hierarchy

~~~text
Recovery readiness identity
→ confirmed factors
→ shared failure-domain findings
→ stale / needs-review factors
→ secret-location metadata
→ maintenance / verification
→ contextual actions
~~~

Do not lead with a score.

## 5. Header

Title:

> 恢复准备

Subtitle:

> 看清已确认恢复方式、共同故障点和仍需核对的地方。

Truth boundary:

> 未记录 ≠ 没有；多个方式 ≠ 多条独立路径。

## 6. Summary grammar

Allowed summary:

~~~text
已确认恢复因素     4
需要核对           2
共享故障点         1
独立路径           未能确认 / N only when solver proves it
~~~

Forbidden:
- 92% 安全;
- 恢复健康分;
- “有 3 种方式所以很安全”;
- missing → zero.

## 7. Factor groups

Group factors by consumer meaning, not internal enums alone:

~~~text
设备上的验证
号码与邮箱
安全密钥
恢复码 / 离线凭据
恢复联系人
服务商重新验证
~~~

Each factor row can show:
- factor kind;
- target account/service;
- carrier object;
- portability;
- last verified;
- state;
- known shared FailureDomain finding.

Example:

~~~text
Passkey
Google Account
同步方式：服务商同步
共同故障点：Google Account provider
最近核对：2026-09-28
~~~

## 8. Shared-failure finding

Preferred UI:

> 短信验证码、TOTP 和 Push Approval 都依赖同一台手机。

Explain:
- affected factors;
- shared domain;
- what fact/evidence supports it;
- unknown assumptions.

Do not expose raw min-cut/graph terminology by default.

## 9. SecretLocator presentation

Only metadata:

~~~text
恢复码
已记录存在
存放：密码管理器
最近核对：2026-09-01
~~~

Masked mode:

~~~text
恢复码
已记录存在
存放：已隐藏
~~~

Never:
- reveal value;
- copy value;
- show QR;
- ask user to paste secret.

## 10. Stale information

Stale recovery metadata is actionable but not automatically false.

Row state:

~~~text
需要核对
最近核对时间过久
服务商政策已更新
载体已归档
~~~

CTA:

> 去核对

The verification action must eventually write authoritative freshness/evidence,
not just close the warning locally.

## 11. Contextual improvement actions

Possible future actions:

~~~text
核对现有恢复方式
建立独立备用方式
更换承载设备
添加安全密钥
核对恢复码存放位置
更新恢复联系人
~~~

Every action must be gated by real production capability.

If no executable workflow exists, show explanation only.

## 12. ProviderPolicy boundary

Provider capability can explain:

> 这个服务支持恢复联系人。

It cannot display:

> 你已经配置恢复联系人。

until Personal Reality confirms it.

Provider policy freshness and user-factor freshness are separate.

## 13. Passkeys

Consumer distinction:

~~~text
Passkey · 服务商同步
Passkey · 绑定设备
Passkey · 同步方式未知
~~~

Do not use simply:

> Passkey = safe backup.

Synced passkeys may share provider account failure domains.

## 14. Device context

If many factors are carried by one device, the UI should surface one correlated
finding rather than duplicate warnings per factor.

Example:

> 这台手机同时承载 4 个登录/恢复方式。

Device Detail may deep-link back to the grouped finding.

## 15. Identity Context composition

When Identity Context becomes Canonical, Preparedness can filter:

~~~text
全部
中国主身份
英国金融身份
工作身份
~~~

Context membership does not change underlying factor/path truth.

## 16. Region composition

Region is another independent filter.

~~~text
Context = UK Financial
∩ Region = GB
~~~

does not generate new recovery relations.

## 17. Incident transition

If a user declares an actual failure:

~~~text
Preparedness
→ explicit “something is unavailable”
→ create/draft RecoveryIncident
→ Recovery solver
~~~

Do not turn a stale warning into an active incident automatically.

## 18. Records boundary

Records owns completed/verified evidence history.

Preparedness owns current readiness/freshness state.

Therefore:
- “Recovery factor verified on 2026-09-28” may appear in Records as evidence;
- “Factor now stale / needs review” belongs in Preparedness/Now.

## 19. Now boundary

Now can surface:
- factor review due;
- stale recovery information;
- confirmed shared failure finding needing action.

Now should deep-link to Preparedness/focused object only after the route is enabled.

Until then, use existing Weaknesses/objects without a ghost route.

## 20. Accessibility / privacy

- all factor rows have text labels, not icon-only meaning;
- sensitive location/provider metadata honors masking;
- status never relies only on color;
- 48dp target minimum;
- no secret material in content descriptions.

## 21. Runtime acceptance after implementation

Required:
- factor list renders only confirmed factors;
- proposed factor cannot appear as confirmed;
- shared domain warning matches core result;
- missing independence displays unknown;
- masked SecretLocator hides sensitive hint;
- no secret content in accessibility tree;
- System Back and Header Up semantics correct;
- five primary nav remains unchanged.

## 22. Stop line

~~~text
RECOVERY_PREPAREDNESS_UX = DESIGN_FROZEN
RECOVERY_PREPAREDNESS_ROUTE = HIDDEN
FACTOR_CANONICAL = REQUIRED
NO_SCORE = REQUIRED
NO_NEW_PRIMARY_TAB = REQUIRED
~~~