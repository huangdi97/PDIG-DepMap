# Identity & Recovery Lens UX Contract

> Date: 2026-10-09  
> Status: **DESIGN_COMPLETE / VISIBILITY_GATED**
>
> This contract completes the consumer UX design for two long-term Product Lenses
> without exposing capabilities that do not yet have governed runtime truth.
>
> Primary IA remains:
>
> ```text
> 现在 / 基础设施 / 变更 / 记录 / 我
> ```
>
> Identity and Recovery do not become extra primary tabs.

## 1. Capability gate

Current visibility:

| Lens | Design | Required truth/runtime | Current visible UI |
| --- | --- | --- | --- |
| Region | complete | current region/spatial projection | YES |
| Dependency | complete | confirmed dependencies / Impact | YES |
| Change | complete | supported ChangePlan scenarios | YES |
| Identity | complete | governed IdentityContext membership | **NO** |
| Recovery | complete | active incident model + recovery solver | **NO** |

Android source gate:
`uivnext/lens/VNextLensAvailability.kt`.

A future screen/entry must check the product capability decision, not merely whether
a design file exists.

## 2. Identity Lens — future consumer question

> **“我现在在看哪个生活身份，以及哪些基础设施真正属于它？”**

### Entry placement after Canonical support

Infrastructure:
- scope selector beside Region/filter context;
- context affects which objects are shown;
- context does not add a nav level.

Me:
- “我的身份上下文” section;
- create/review/rename/archive contexts;
- pending membership proposals clearly separate from confirmed members.

Search:
- search confirmed context names;
- entering a context applies a scope, not a fake dependency.

### Compact hierarchy

```text
Context name
purpose / scope
confirmed member summary
pending review
object groups
dependency / Impact entry
region intersection
```

### Medium / Expanded

```text
Context list / scope rail
          │
          ▼
Context workspace
  identity
  members
  region distribution
  dependency / Impact
  active changes
  pending membership
```

Do not turn wide Identity into an admin table.

## 3. Identity membership states

Consumer language:

```text
confirmed        → 已确认属于此身份
proposal         → 建议加入 · 待确认
rejected         → 不展示在日常 Lens；保留审计
archived context → 已归档
```

Forbidden:
- confidence % as membership truth;
- “自动整理完成” when proposals remain;
- region/provider similarity presented as confirmed membership.

## 4. Identity empty states

No confirmed contexts:

> 尚未建立身份上下文。你仍可以按地区和基础设施类型管理已有记录。

No members in a confirmed context:

> 这个身份还没有已确认成员。未记录不代表现实中不存在相关对象。

Only proposals:

> 有待确认的建议；确认前不会进入这个身份的正式视图。

No “nothing here = safe/complete”.

## 5. Region × Identity

When both are supported:

```text
身份：英国金融
地区：英国
```

is an intersection query.

UI must expose active scope and allow clearing each dimension independently.

Do not persist:

```text
"英国金融@英国"
```

as a new graph object merely because the user combined filters.

## 6. Recovery Lens — future consumer question

> **“某些东西已经不能用了，我现在还剩什么办法？”**

Recovery only becomes visible after:
- explicit Incident Context support;
- recovery solver;
- FailureDomain integration;
- RecoveryCycle integration;
- provider/time-constraint integration;
- cross-platform conformance.

Until then, do **not** create a decorative “紧急恢复” page.

## 7. Recovery entry placement

Future contextual entries:

### Object detail

When supported:

```text
我已经失去这个设备 / 号码 / 身份因素
```

This starts incident declaration/review. It does not silently mark the object lost.

### Now

An **active confirmed incident** may appear before ordinary maintenance:

```text
恢复中
2 个因素已确认不可用
还有 1 条已确认独立路径
```

Only the solver may provide the path statement.

### Me

“恢复准备” is allowed as preparedness/configuration after the underlying facts are
governed. It is not active Recovery Mode.

## 8. Recovery incident declaration

Before computing anything, UI asks what is unavailable.

Example:

```text
发生了什么？
☐ 手机丢失
☐ 安全密钥不可用
☐ 邮箱无法访问
☐ 其他
```

Actual implementation should select concrete recorded objects, not free-form labels.

Review step:

```text
将标记为“当前不可用”
Pixel 8
+86 138****8823

这会创建恢复事件，不会删除这些基础设施记录。
```

User must explicitly confirm the incident.

## 9. Recovery result hierarchy

Once solver-backed:

```text
Incident state
→ confirmed unavailable factors
→ surviving recovery roots
→ independent-path context
→ blocked / cycle-dependent options
→ provider/time constraints
→ next safe actions
→ verify recovered access
```

### Highest emphasis

“What can I actually use next?”

Not a graph for graph's sake.

## 10. Recovery root card

A viable root card needs:

```text
factor identity
availability evidence
what it can recover
failure-domain context
provider constraints
verification/freshness
```

Example copy:

> 安全密钥 · 已确认可用  
> 可用于 GitHub 登录认证  
> 与丢失手机不共享已确认故障域

Only show the last line if FailureDomain evidence proves it.

## 11. Blocked option

Example:

> 短信验证码 · 当前不可用  
> 依赖已声明丢失的主手机号

Or:

> 备用 App 验证 · 需要核对  
> 与主 App 可能共享同一设备故障域

Potential/suspected domains must not be presented as confirmed.

## 12. Recovery cycle UI

Confirmed cycle:

> **恢复链形成循环**  
> A 需要 B，B 又需要 A。它不能作为独立恢复根。

Potential cycle:

> **可能存在恢复循环 · 待核对**

No-cycle:

> 当前已确认范围内未发现恢复循环。

Never:

> 恢复结构安全。

## 13. Independent paths

Consumer hierarchy:

```text
已确认路径数
已确认独立路径数
待核对的共享故障点
```

Do not collapse into a single “冗余分数”.

If independent count cannot be computed:

```text
独立路径：当前无法判断
```

not 0.

## 14. Provider/time constraints

Provider policy appears as an explanatory constraint:

> 服务商规则显示：此恢复方式可能有等待期。规则最后核验于 …

But:

```text
provider supports recovery contact
!=
user configured recovery contact
```

If policy source is stale/needs_review:

> 服务商规则需要重新核对。

Do not block/allow a user path solely from unverifiable policy.

## 15. Recovery actions

Action classes:

```text
Use
Verify
Wait
Contact provider
Re-proof identity
Bind replacement
Revoke compromised factor
Review settings
```

Actions may open external provider guidance until PDIG has an authorized connector.

“完成” and “验证” remain separate.

## 16. Security / secrets

The UI may say:

```text
已记录：有离线恢复码
位置提示：家庭保险箱
最近核对：2026-08
```

It must not display/store the actual recovery code in standard graph fields.

Sensitive UI follows workspace masking.

Screenshots/evidence must not contain real secrets.

## 17. Accessibility

Recovery is high-stress but still follows ordinary accessible interaction:
- no color-only path state;
- blocked reasons read by screen reader;
- action order linear and keyboard reachable;
- no flashing emergency animation;
- reduce-motion honored;
- diagrams have a list equivalent.

## 18. Failure/unknown states

Solver unavailable:

> 当前无法计算恢复路径。你的记录不会被自动解释为安全或不可恢复。

Insufficient evidence:

> 现有记录不足以判断哪些路径仍可用。先核对可访问的设备、号码和邮箱。

All confirmed paths blocked:

> 在当前已确认信息中，没有可用恢复路径。仍可能存在尚未记录的方法。

The last sentence is mandatory when unknown scope remains.

## 19. Records integration

Recovery events that actually occur belong in Records:

```text
incident activated
factor marked unavailable
recovery action completed
recovery action verified
replacement authenticator bound
incident resolved
```

A draft incident/what-if does not become history.

## 20. Change integration

After successful recovery, the UI may offer supported changes:

```text
更换丢失手机号
替换支付卡
移除失效设备
增加独立恢复方式
```

Only actual supported ChangePrimitive/Scenario actions are clickable.

## 21. No new primary tab

Permanent product rule:

```text
Lens growth != tab growth
```

Identity belongs as scope/context within Infrastructure/Me.
Recovery belongs to incident/object context.

The five-primary IA remains stable.

## 22. Runtime gates before visibility

Identity:
```text
Canonical context schema
membership authority
repository/query
conformance
production read model
Android runtime evidence
→ visible Identity entry
```

Recovery:
```text
Incident schema
solver
FailureDomain/Cycle integration
provider constraint mapping
conformance
production adapter
security tests
Android runtime evidence
→ visible Recovery entry
```

Until then:

```text
IDENTITY_UX_DESIGN = COMPLETE
IDENTITY_VISIBLE = NO

RECOVERY_UX_DESIGN = COMPLETE
RECOVERY_VISIBLE = NO
```
