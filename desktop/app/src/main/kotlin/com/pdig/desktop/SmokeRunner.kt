package com.pdig.desktop

import com.pdig.app.data.DetectedParty
import com.pdig.app.data.ImportPreview
import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.core.domain.ActionVerificationStatus as DomainVerificationStatus
import com.pdig.core.generated.NodeKind
import com.pdig.core.sources.Observation
import com.pdig.core.sources.ObservationDirection
import com.pdig.core.sources.WechatParser
import com.pdig.desktop.data.DesktopSession
import com.pdig.desktop.persist.DepmapFileStore
import java.io.File
import java.time.Instant

/**
 * `--smoke` headless runtime driver（E8 证据）。在临时目录真实执行：
 * fresh launch / create-open / import / proposal / candidate / drift / 三 scenario /
 * backup / restore / close / reopen / delete data，0 crash、0 corrupted data。
 *
 * 任一 step FAIL → 退出码 1；全部 PASS → 0。
 */
object SmokeRunner {

    private var failures = 0

    fun run(repoRoot: File, workDir: File): Int {
        val fixture = File(repoRoot, "fixtures/import/normal-wechat.csv")
        check(fixture.isFile) { "wechat fixture missing: $fixture" }

        step("fresh-launch") {
            val s = DesktopSession.open()
            check(s.graph.graphRevision() == 0) { "fresh session revision != 0" }
            s.close()
        }

        val dataFile = File(workDir, "smoke.depmap")
        val backupFile = File(workDir, "smoke-backup.depmap")
        val password = "smoke-password-2026"
        val store = DepmapFileStore()

        step("create-open") {
             val s = DesktopSession.open()
             val manualObs = manualObservations()
             val preview = s.sources.previewImport(manualObs, emptyList(), "manual", "smoke manual")
             check(preview.instruments.isNotEmpty()) { "no instrument detected from manual obs" }
             val expectedCardId = preview.instruments.first().nodeId
             s.sources.commitImport(preview)
            store.save(dataFile, s.exportPayload(), password)
            s.close()
            val s2 = DesktopSession.restore(store.open(dataFile, password))
             check(s2.graph.nodes().any { it.id == expectedCardId }) { "node not restored after open" }
            s2.close()
        }

        step("wrong-password-rejected") {
            val failed = try {
                store.open(dataFile, "not-the-password")
                false
            } catch (e: DepmapFileStore.StoreException) {
                e.code == "auth_failed"
            }
            check(failed) { "wrong password must fail with auth_failed" }
        }

         step("tampered-file-rejected") {
             val original = dataFile.readText()
             val marker = "\"ciphertext\":\""
             val idx = original.indexOf(marker)
             check(idx > 0) { "ciphertext field not found in container" }
             val flipAt = idx + marker.length + 2
             val flipped = if (original[flipAt] == 'A') 'B' else 'A'
             val tamperedText = original.substring(0, flipAt) + flipped + original.substring(flipAt + 1)
             val tampered = File(workDir, "tampered.depmap")
             tampered.writeText(tamperedText)
             val failed = try {
                 store.open(tampered, password)
                 false
             } catch (e: DepmapFileStore.StoreException) {
                 e.code == "auth_failed"
             }
             check(failed) { "tampered ciphertext must fail GCM authentication (auth_failed)" }
         }
 
         step("future-schema-rejected") {
             val payload = store.open(dataFile, password)
             val futurePayload = payload.replace("\"schemaVersion\":3", "\"schemaVersion\":99")
             check(futurePayload != payload) { "future-schema payload mutation did not apply" }
             val newer = File(workDir, "future.depmap")
             store.save(newer, futurePayload, password)
             val failed = try {
                 store.open(newer, password)
                 false
             } catch (e: DepmapFileStore.StoreException) {
                 e.code == "future_schema"
             }
             check(failed) { "future schema must be rejected" }
         }

        var session = DesktopSession.restore(store.open(dataFile, password))

        step("import-wechat-fixture") {
            val parsed = WechatParser.parse(fixture.readBytes())
            check(parsed.observations.isNotEmpty()) { "wechat parse produced 0 observations" }
            val preview = session.sources.previewImport(
                parsed.observations,
                parsed.errors.map { "${it.line}:${it.reason}" },
                "wechat_statement",
                "smoke wechat",
            )
            check(preview.instruments.isNotEmpty()) { "no instruments detected" }
            val result = session.sources.commitImport(preview)
            check(result.nodeCount >= 1) { "commit import created no nodes" }
            check(result.proposalCount >= 1) { "commit import created no proposals" }
        }

        step("proposal-accept-bumps-revision") {
            val before = session.graph.graphRevision()
            val pending = session.proposals.pendingProposals()
            check(pending.isNotEmpty()) { "no pending proposals" }
            session.proposals.acceptProposal(pending.first().id)
            val after = session.graph.graphRevision()
            check(session.proposals.pendingProposals().none { it.id == pending.first().id }) { "accepted proposal still pending" }
            val proposalDeps = session.driver.prepare("SELECT COUNT(*) AS c FROM dependencies WHERE origin = 'proposal'").get()?.long("c") ?: 0L; check(proposalDeps > 0) { "accept created no proposal-origin dependency" }
            check(after > before) { "proposal accept must bump graphRevision" }
        }

        step("criticality-required-only-by-user") {
            val dep = session.graph.dependencies().first()
            check(dep.criticality == "unknown") { "machine-created dependency must be criticality=unknown" }
            session.graph.setDependencyCriticality(dep.id, required = true)
            check(session.graph.dependencies().first { it.id == dep.id }.criticality == "required") {
                "user must be able to set required"
            }
        }
 
        runScenarios("replace_payment_card", session)
        runScenarios("expiring_payment_card", session)
        runScenarios("close_payment_instrument", session)
        runScenarios("replace_phone_number", session)

        // 引擎证据链（候选/漂移）放到 scenario 之后：额外 funding 边不得干扰 must_change 判定。
        SmokeEngineSteps.run(session, ::step)

        step("candidate-accept-dismiss") {
            val now = Instant.now().toString()
            session.driver.exec(
                """INSERT INTO discovery_candidates
                   (id, candidate_kind, display_label, normalized_key, source_instance_id, evidence_refs_json,
                    observation_count, first_seen_at, last_seen_at, status, created_at, updated_at)
                   VALUES ('cand-smoke-1','service','Smoke Merchant Co','smoke-merchant-co','legacy-wechat-statement',
                           '[]', 3, '$now', '$now', 'pending', '$now', '$now')""",
            )
            session.driver.exec(
                """INSERT INTO discovery_candidates
                   (id, candidate_kind, display_label, normalized_key, source_instance_id, evidence_refs_json,
                    observation_count, first_seen_at, last_seen_at, status, created_at, updated_at)
                   VALUES ('cand-smoke-2','service','Ignore Me Corp','ignore-me-corp','legacy-wechat-statement',
                           '[]', 2, '$now', '$now', 'pending', '$now', '$now')""",
            )
            check(session.candidates.pendingCandidates().size >= 2) { "candidates not visible" }
            val nodeId = session.candidates.acceptCandidate("cand-smoke-1")
            check(nodeId.isNotEmpty()) { "acceptCandidate returned empty nodeId" }
            check(session.graph.nodes().any { it.id == nodeId }) { "accepted candidate did not create node" }
            session.candidates.dismissCandidate("cand-smoke-2")
            check(session.candidates.pendingCandidates().none { it.id == "cand-smoke-2" }) { "dismiss did not clear pending" }
        }

        step("drift-resolve-dismiss") {
            val now = Instant.now().toString()
            val depId = session.graph.dependencies().first().id
            session.driver.exec(
                """INSERT INTO reality_drifts
                   (id, kind, target_node_id, capability, candidate_from, candidate_relation,
                    related_dependency_ids_json, evidence_refs_json, proposal_keys_json,
                    observation_count, detected_at, updated_at, status)
                   VALUES ('drift-smoke-1','possible_replacement','nd-card-x','payment','nd-card-y','funding_source',
                           '["$depId"]','[]','[]', 4, '$now', '$now', 'open')""",
            )
            check(session.drifts.openDrifts().any { it.id == "drift-smoke-1" }) { "drift not visible" }
            session.drifts.resolveDriftAsReplacement("drift-smoke-1")
            check(session.drifts.openDrifts().none { it.id == "drift-smoke-1" }) { "resolved drift still open" }
            session.driver.exec(
                """INSERT INTO reality_drifts
                   (id, kind, target_node_id, capability, candidate_from, candidate_relation,
                    related_dependency_ids_json, evidence_refs_json, proposal_keys_json,
                    observation_count, detected_at, updated_at, status)
                   VALUES ('drift-smoke-2','possible_additional_path','nd-card-x','payment','nd-card-z','funding_source',
                           '[]','[]','[]', 2, '$now', '$now', 'open')""",
            )
            session.drifts.dismissDrift("drift-smoke-2")
            check(session.drifts.openDrifts().none { it.id == "drift-smoke-2" }) { "dismissed drift still open" }
        }


        step("backup-restore-reopen-delete") {
            store.save(dataFile, session.exportPayload(), password)
            store.backupCopy(dataFile, backupFile, overwrite = true)
            check(backupFile.isFile) { "backup file missing" }
            val restored = DesktopSession.restore(store.open(backupFile, password))
            check(restored.graph.nodes().size == session.graph.nodes().size) { "restored node count mismatch" }
            restored.close()
            session.close()
            session = DesktopSession.restore(store.open(dataFile, password))
            check(session.graph.nodes().isNotEmpty()) { "reopen produced empty graph" }
            session.close()
            check(dataFile.delete()) { "data file delete failed" }
            check(backupFile.delete()) { "backup delete failed" }
        }

        if (failures == 0) {
            println("[smoke] VERDICT: PASS (all steps)")
            return 0
        }
        println("[smoke] VERDICT: FAIL ($failures step(s))")
        return 1
    }
    private fun runScenarios(scenarioId: String, session: DesktopSession) {
        step("scenario-$scenarioId") {
            val nodes = session.graph.nodes()
            val activeDeps = session.graph.dependencies().filter { it.state == "active" }
            val target = if (scenarioId == "replace_phone_number") {
                // v0.3.0：手机号场景的目标是身份锚点（identity_anchor）；无锚点时先创建一个
                val existing = nodes.firstOrNull { it.kind == com.pdig.core.generated.NodeKind.IDENTITY_ANCHOR.wire }
                if (existing != null) {
                    existing
                } else {
                    val now = Instant.now().toString()
                    session.driver.exec(
                        "INSERT INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at) " +
                            "VALUES ('smoke-phone-anchor','identity_anchor','旧手机号',0,'{}','self','$now','$now')",
                    )
                    checkNotNull(session.graph.nodes().firstOrNull { it.id == "smoke-phone-anchor" })
                }
            } else {
                activeDeps.firstOrNull { d -> nodes.any { it.id == d.from } }
                    ?.let { d -> nodes.first { it.id == d.from } }
                    ?: nodes.first { it.kind == NodeKind.PAYMENT_INSTRUMENT.wire }
            }
            if (scenarioId != "replace_phone_number") {
                // 确保 target 至少一条 required 支付依赖 → impact 必产生 must_change（unknown→required 只能由用户路径设置）
                val targetActive = activeDeps.filter { it.from == target.id }
                if (targetActive.isNotEmpty()) {
                    val dep = targetActive.first()
                    if (dep.criticality != "required") {
                        session.graph.setDependencyCriticality(dep.id, required = true)
                    }
                }
            }
            val planId = session.plans.createPlanForScenario(
                ScenarioPlanRequest(scenarioId = scenarioId, targetNodeId = target.id, effectiveDate = null),
            )
            val detail = checkNotNull(session.plans.planDetail(planId)) { "planDetail null" }
            check(detail.actions.isNotEmpty()) { "plan produced no actions" }
            if (scenarioId == "replace_phone_number") {
                // Make-Before-Break DAG gate：前置未完成时完成后续动作必须被拒绝（action_missing_prerequisite）
                val sequential = listOf("rpn-prepare-1", "rpn-change-1", "rpn-verify-1", "rpn-change-2")
                val actionIds = detail.actions.map { it.id }
                check(actionIds.containsAll(sequential)) { "replace_phone_number plan missing DAG actions" }
                for (i in 0 until sequential.size - 1) {
                    val cur = sequential[i]
                    val next = sequential[i + 1]
                    // 前置未完成 → 完成 next 必须被拒绝
                    val blocked = try {
                        session.plans.completeAction(planId, next)
                        false
                    } catch (e: Throwable) {
                        e.message?.contains("action_missing_prerequisite") == true
                    }
                    check(blocked) { "make-before-break gate not enforced at $next (before $cur done)" }
                    session.plans.completeAction(planId, cur)
                }
                session.plans.completeAction(planId, sequential.last())
                val done = checkNotNull(session.plans.planDetail(planId))
                check(done.actions.all { it.done }) { "replace_phone_number DAG not fully completable" }
            } else {
                // 完成每个 CHANGE action；done ≠ verified 断言
                for (action in detail.actions) {
                    if (action.resolvesImpactKeys.isNotEmpty()) {
                        session.plans.completeAction(planId, action.id)
                        val after = checkNotNull(session.plans.planDetail(planId))
                        val updated = after.actions.first { it.id == action.id }
                        check(updated.done) { "action not marked done" }
                        check(updated.verification?.status != DomainVerificationStatus.VERIFIED) {
                            "done must NOT imply verified"
                        }
                    }
                }
                // 可验证动作置 VERIFIED（人工确认路径）
                val d2 = checkNotNull(session.plans.planDetail(planId))
                for (action in d2.actions) {
                    val v = action.verification
                    if (action.done && v != null && v.status != DomainVerificationStatus.NOT_REQUIRED) {
                        session.plans.verifyAction(planId, action.id)
                    }
                }
            }
            val d3 = checkNotNull(session.plans.planDetail(planId))
            check(d3.workflowState != com.pdig.core.generated.ChangePlanWorkflowState.DRAFT) { "plan did not leave draft" }
        }
    }

    private fun manualObservations(): List<Observation> = listOf(
        Observation(
            occurredAt = "2026-09-01T00:00:00Z",
            description = "smoke manual txn",
            amount = 1.0,
            currency = "CNY",
             direction = ObservationDirection.OUT,
             source = "manual",
             sourceTxnId = "",
             merchantTxnId = "",
             merchantRaw = "Card-X",
            paymentMethodRaw = "Card-X",
            status = "",
            note = "",
        ),
    )

    private fun step(name: String, block: () -> Unit) {
        try {
            block()
            println("[smoke] PASS $name")
        } catch (e: Throwable) {
            failures++
            println("[smoke] FAIL $name :: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}