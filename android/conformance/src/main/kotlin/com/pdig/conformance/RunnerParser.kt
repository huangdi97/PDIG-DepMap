package com.pdig.conformance

import com.pdig.core.json.Json
import com.pdig.core.scenario.ScenarioRegistry
import com.pdig.core.schema.SchemaVersion
import com.pdig.core.sources.GenericCsvParser
import com.pdig.core.sources.OfxParser
import com.pdig.core.sources.ParseResult
import com.pdig.core.sources.WechatParser
import com.pdig.core.statemachine.CandidateMachine
import com.pdig.core.statemachine.ChangePlanMachine
import com.pdig.core.statemachine.DriftMachine
import com.pdig.core.statemachine.GraphRevisionMachine
import com.pdig.core.statemachine.VerificationMachine
import java.io.File

// ---------------------------------------------------------------------------
// parser —— 真实账单文件解析（只读 fixture，不落库）
// ---------------------------------------------------------------------------

internal fun runParser(input: Json.Obj): Json {
    val adapter = str(input, "adapterId")
    val fileName = str(input, "file")
    val file = File(ROOT, "fixtures/import/$fileName")
    if (!file.exists()) error("import fixture not found: ${file.absolutePath}")
    val data = file.readBytes()
    val result: ParseResult = when (adapter) {
        "wechat" -> WechatParser.parse(data)
        "ofx_qfx" -> OfxParser.parse(data)
        "generic_csv" -> GenericCsvParser.parse(data, parseMapping(input["mapping"]))
        else -> error("unknown adapter: $adapter")
    }
    return Json.Obj(
        listOf(
            "observationCount" to Json.Num(result.observations.size.toString()),
            "observations" to Json.Arr(
                result.observations.map {
                    Json.Obj(
                        listOf(
                            "occurredAt" to Json.Str(it.occurredAt),
                            "amount" to num(it.amount),
                            "currency" to Json.Str(it.currency),
                            "direction" to Json.Str(it.direction.wire),
                            "merchantRaw" to Json.Str(it.merchantRaw),
                            "status" to Json.Str(it.status),
                        ),
                    )
                },
            ),
            "errorCount" to Json.Num(result.errors.size.toString()),
            "errors" to Json.Arr(
                result.errors.map {
                    Json.Obj(
                        listOf(
                            "line" to Json.Num(it.line.toString()),
                            "reason" to Json.Str(it.reason),
                        ),
                    )
                },
            ),
        ),
    )
}

// ---------------------------------------------------------------------------
// scenario / migration（产品配置与版本契约）
// ---------------------------------------------------------------------------

internal fun runScenario(): Json = Json.Obj(
    listOf(
        "activeIds" to Json.Arr(ScenarioRegistry.active.map { Json.Str(it.id) }),
        "activeCategories" to Json.Arr(ScenarioRegistry.active.map { Json.Str(it.category) }),
        "plannedIds" to Json.Arr(ScenarioRegistry.planned.map { Json.Str(it.id) }),
        "plannedExecutable" to Json.Arr(
            ScenarioRegistry.planned.map { Json.Bool(it.availability == "active") },
        ),
        "leadingTimeByTemplate" to Json.Obj(
            ScenarioRegistry.active.map {
                it.id to (it.recommendedLeadTimeDays?.let { d -> Json.Num(d.toString()) } ?: Json.Null)
            },
        ),
    ),
)

internal fun runMigration(): Json = Json.Obj(
    listOf(
        "migrations" to Json.Arr(SchemaVersion.migrations.map { Json.Num(it.toString()) }),
        "payloadKind" to Json.Str(SchemaVersion.payloadKind),
        "payloadTables" to Json.Arr(SchemaVersion.payloadTables.map { Json.Str(it) }),
        "rejectedSchemaVersions" to Json.Arr(SchemaVersion.rejectedSchemaVersions.map { Json.Num(it.toString()) }),
        "rejectedPayloadVersions" to Json.Arr(SchemaVersion.rejectedPayloadVersions.map { Json.Num(it.toString()) }),
        "graphRevisionNeverBumpedBy" to Json.Arr(SchemaVersion.graphRevisionNeverBumpedBy.map { Json.Str(it) }),
    ),
)

// ---------------------------------------------------------------------------
// state machines（Kotlin 权威定义 → 与 spec 比对）
// ---------------------------------------------------------------------------

internal fun runStateMachine(id: String): Json = when (id) {
    "state-machine-change-plan" -> Json.Obj(
        listOf(
            "transitions" to Json.Obj(
                ChangePlanMachine.transitions.map { (from, tos) ->
                    from.wire to Json.Arr(tos.map { Json.Str(it.wire) })
                },
            ),
            "terminal" to Json.Arr(ChangePlanMachine.terminal.map { Json.Str(it.wire) }),
            "derivedStatus" to Json.Obj(
                listOf(
                    "name" to Json.Str(ChangePlanMachine.derivedStatus.name),
                    "value" to Json.Str(ChangePlanMachine.derivedStatus.value),
                    "rule" to Json.Str(ChangePlanMachine.derivedStatus.rule),
                    "persisted" to Json.Bool(ChangePlanMachine.derivedStatus.persisted),
                ),
            ),
            "guards" to Json.Arr(
                ChangePlanMachine.guards.map {
                    Json.Obj(
                        listOf(
                            "id" to Json.Str(it.id),
                            "rule" to Json.Str(it.rule),
                            "errorCode" to Json.Str(it.errorCode),
                        ),
                    )
                },
            ),
        ),
    )

    "state-machine-reality-drift" -> Json.Obj(
        listOf(
            "initial" to Json.Str(DriftMachine.initial.wire),
            "transitions" to Json.Obj(
                DriftMachine.transitions.map { (from, tos) -> from.wire to Json.Arr(tos.map { Json.Str(it.wire) }) },
            ),
            "creationRule" to Json.Obj(
                listOf(
                    "requires" to Json.Str(DriftMachine.creationRule.requires),
                    "minObservations" to Json.Num(DriftMachine.creationRule.minObservations.toString()),
                    "absenceOnly" to Json.Str(DriftMachine.creationRule.absenceOnly),
                    "alreadyConfirmedSource" to Json.Str(DriftMachine.creationRule.alreadyConfirmedSource),
                    "belowThreshold" to Json.Str(DriftMachine.creationRule.belowThreshold),
                    "duplicateEvidenceRef" to Json.Str(DriftMachine.creationRule.duplicateEvidenceRef),
                    "upsert" to Json.Str(DriftMachine.creationRule.upsert),
                ),
            ),
            "guards" to Json.Arr(
                DriftMachine.guards.map {
                    Json.Obj(
                        listOf("id" to Json.Str(it.id), "rule" to Json.Str(it.rule), "errorCode" to Json.Str(it.errorCode)),
                    )
                },
            ),
        ),
    )

    "state-machine-discovery-candidate" -> Json.Obj(
        listOf(
            "initial" to Json.Str(CandidateMachine.initial.wire),
            "transitions" to Json.Obj(
                CandidateMachine.transitions.map { (from, tos) -> from.wire to Json.Arr(tos.map { Json.Str(it.wire) }) },
            ),
            "accept" to Json.Obj(
                listOf(
                    "mutatesReality" to Json.Bool(CandidateMachine.accept.mutatesReality),
                    "effects" to Json.Arr(CandidateMachine.accept.effects.map { Json.Str(it) }),
                    "bumpsGraphRevision" to Json.Bool(CandidateMachine.accept.bumpsGraphRevision),
                    "replay" to Json.Str(CandidateMachine.accept.replay),
                ),
            ),
            "dismiss" to Json.Obj(
                listOf(
                    "mutatesReality" to Json.Bool(CandidateMachine.dismiss.mutatesReality),
                    "effects" to Json.Arr(CandidateMachine.dismiss.effects.map { Json.Str(it) }),
                    "reappeal" to Json.Str(CandidateMachine.dismiss.reappeal),
                ),
            ),
            "guards" to Json.Arr(
                CandidateMachine.guards.map {
                    Json.Obj(
                        listOf("id" to Json.Str(it.id), "rule" to Json.Str(it.rule), "errorCode" to Json.Str(it.errorCode)),
                    )
                },
            ),
        ),
    )

    "state-machine-action-verification" -> Json.Obj(
        listOf(
            "initial" to Json.Str(VerificationMachine.initial.wire),
            "terminal" to Json.Arr(VerificationMachine.terminal.map { Json.Str(it.wire) }),
            "transitions" to Json.Obj(
                VerificationMachine.transitions.map { (from, tos) -> from.wire to Json.Arr(tos.map { Json.Str(it.wire) }) },
            ),
            "evidenceSignalRule" to Json.Obj(
                listOf(
                    "appliesOnlyTo" to Json.Arr(
                        VerificationMachine.evidenceSignalRule.appliesOnlyTo.map { Json.Str(it.wire) },
                    ),
                    "requiresMethod" to Json.Str(VerificationMachine.evidenceSignalRule.requiresMethod),
                    "requiresMatch" to Json.Str(VerificationMachine.evidenceSignalRule.requiresMatch),
                    "effect" to Json.Str(VerificationMachine.evidenceSignalRule.effect),
                    "never" to Json.Arr(VerificationMachine.evidenceSignalRule.never.map { Json.Str(it) }),
                ),
            ),
            "guards" to Json.Arr(
                VerificationMachine.guards.map {
                    Json.Obj(
                        listOf("id" to Json.Str(it.id), "rule" to Json.Str(it.rule), "errorCode" to Json.Str(it.errorCode)),
                    )
                },
            ),
        ),
    )

    "state-machine-graph-revision" -> Json.Obj(
        listOf(
            "initial" to Json.Num(GraphRevisionMachine.initial.toString()),
            "monotonic" to Json.Bool(GraphRevisionMachine.monotonic),
            "atomicity" to Json.Str(GraphRevisionMachine.atomicity),
            "bumpsOn" to Json.Arr(GraphRevisionMachine.bumpsOn.map { Json.Str(it) }),
            "neverBumpsOn" to Json.Arr(GraphRevisionMachine.neverBumpsOn.map { Json.Str(it) }),
        ),
    )

    else -> throw NotImplementedError("no state machine runner for $id")
}