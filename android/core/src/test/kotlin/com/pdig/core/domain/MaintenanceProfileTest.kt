package com.pdig.core.domain

import com.pdig.core.generated.MaintenanceCadenceKind
import com.pdig.core.generated.MaintenanceFactKind
import com.pdig.core.generated.MaintenanceFactState
import com.pdig.core.generated.MaintenanceScheduleKind
import com.pdig.core.generated.MaintenanceScheduleState
import com.pdig.core.generated.NodeKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MaintenanceProfileTest {
    @Test
    fun cardFactsAndSchedulesDecodeFromGovernedProfile() {
        val fields = """
            {
              "maintenance_profile": {
                "version": 1,
                "facts": [
                  {
                    "id": "fee",
                    "kind": "card_annual_fee_amount",
                    "value_type": "decimal_string",
                    "value": "100",
                    "state": "confirmed",
                    "verification_basis_type": "user_confirmed",
                    "confirmed_at": "2026-10-10T00:00:00Z"
                  },
                  {
                    "id": "day",
                    "kind": "card_billing_day",
                    "value_type": "integer",
                    "value": "12",
                    "state": "confirmed",
                    "verification_basis_type": "authoritative_source",
                    "confirmed_at": "2026-10-10T00:00:00Z"
                  }
                ],
                "schedules": [
                  {
                    "id": "annual",
                    "kind": "card_annual_fee_checkpoint",
                    "state": "active",
                    "cadence": {
                      "kind": "yearly_month_day",
                      "month": 11,
                      "day": 20,
                      "overflow_policy": "clamp_to_last_day"
                    },
                    "verification_basis_type": "user_confirmed",
                    "confirmed_at": "2026-10-10T00:00:00Z"
                  }
                ]
              }
            }
        """.trimIndent()

        val profile = governedMaintenanceProfile(NodeKind.PAYMENT_INSTRUMENT, fields)
        assertEquals(2, profile.facts.size)
        assertEquals(MaintenanceFactKind.CARD_ANNUAL_FEE_AMOUNT, profile.facts[0].kind)
        assertEquals(MaintenanceFactState.CONFIRMED, profile.facts[0].state)
        assertEquals(1, profile.schedules.size)
        assertEquals(MaintenanceScheduleKind.CARD_ANNUAL_FEE_CHECKPOINT, profile.schedules[0].kind)
        assertEquals(MaintenanceScheduleState.ACTIVE, profile.schedules[0].state)
        assertEquals(MaintenanceCadenceKind.YEARLY_MONTH_DAY, profile.schedules[0].cadence.kind)
        assertEquals(11, profile.schedules[0].cadence.month)
        assertEquals(20, profile.schedules[0].cadence.day)
    }

    @Test
    fun numberLifecycleRequiresGovernedPhoneSubtype() {
        val maintenance = """
          "maintenance_profile": {
            "version": 1,
            "facts": [{
              "id": "plan",
              "kind": "number_plan_cost",
              "value_type": "decimal_string",
              "value": "5",
              "state": "confirmed",
              "verification_basis_type": "user_confirmed",
              "confirmed_at": "t"
            }],
            "schedules": [{
              "id": "keep",
              "kind": "number_keep_alive",
              "state": "active",
              "cadence": {
                "kind": "interval_days",
                "interval_days": 90,
                "anchor_date": "2026-08-07"
              },
              "verification_basis_type": "user_confirmed",
              "confirmed_at": "t"
            }]
          }
        """.trimIndent()

        val generic = "{${maintenance}}"
        assertTrue(
            governedMaintenanceProfile(NodeKind.IDENTITY_ANCHOR, generic).facts.isEmpty(),
        )
        assertTrue(
            governedMaintenanceProfile(NodeKind.IDENTITY_ANCHOR, generic).schedules.isEmpty(),
        )

        val phone = """
            {
              "identity_anchor_profile": {
                "version": 1,
                "subtype": "phone_number",
                "verification_basis_type": "user_confirmed",
                "confirmed_at": "t"
              },
              ${maintenance}
            }
        """.trimIndent()
        val phoneProfile = governedMaintenanceProfile(NodeKind.IDENTITY_ANCHOR, phone)
        assertEquals(listOf(MaintenanceFactKind.NUMBER_PLAN_COST), phoneProfile.facts.map { it.kind })
        assertEquals(listOf(MaintenanceScheduleKind.NUMBER_KEEP_ALIVE), phoneProfile.schedules.map { it.kind })

        val email = phone.replace(""phone_number"", ""email_address"")
        assertTrue(governedMaintenanceProfile(NodeKind.IDENTITY_ANCHOR, email).facts.isEmpty())
        assertTrue(governedMaintenanceProfile(NodeKind.IDENTITY_ANCHOR, email).schedules.isEmpty())
    }

    @Test
    fun invalidItemsFailClosedWithoutDestroyingValidSiblings() {
        val fields = """
            {
              "maintenance_profile": {
                "version": 1,
                "facts": [
                  {
                    "id": "bad-day",
                    "kind": "card_billing_day",
                    "value_type": "integer",
                    "value": "32",
                    "state": "confirmed",
                    "verification_basis_type": "user_confirmed",
                    "confirmed_at": "t"
                  },
                  {
                    "id": "valid-day",
                    "kind": "card_payment_due_day",
                    "value_type": "integer",
                    "value": "26",
                    "state": "confirmed",
                    "verification_basis_type": "user_confirmed",
                    "confirmed_at": "t"
                  },
                  {
                    "id": "proposal",
                    "kind": "card_autopay_mode",
                    "value_type": "text",
                    "value": "full",
                    "state": "proposal",
                    "verification_basis_type": "user_confirmed",
                    "confirmed_at": "t"
                  }
                ],
                "schedules": [
                  {
                    "id": "bad-cadence",
                    "kind": "card_billing_checkpoint",
                    "state": "active",
                    "cadence": {
                      "kind": "interval_days",
                      "interval_days": 30,
                      "anchor_date": "2026-01-01"
                    },
                    "verification_basis_type": "user_confirmed",
                    "confirmed_at": "t"
                  },
                  {
                    "id": "valid-cadence",
                    "kind": "card_billing_checkpoint",
                    "state": "needs_review",
                    "cadence": {
                      "kind": "monthly_day",
                      "day_of_month": 31,
                      "overflow_policy": "user_confirm"
                    },
                    "verification_basis_type": "user_confirmed",
                    "confirmed_at": "t"
                  }
                ]
              }
            }
        """.trimIndent()

        val profile = governedMaintenanceProfile(NodeKind.PAYMENT_INSTRUMENT, fields)
        assertEquals(listOf("valid-day"), profile.facts.map { it.id })
        assertEquals(listOf("valid-cadence"), profile.schedules.map { it.id })
        assertEquals(MaintenanceScheduleState.NEEDS_REVIEW, profile.schedules.single().state)
    }

    @Test
    fun valueTypesAreKindSpecificAndNeverGuessedFromText() {
        val fields = """
            {
              "maintenance_profile": {
                "version": 1,
                "facts": [
                  {"id":"fee-as-text","kind":"card_annual_fee_amount","value_type":"text","value":"100","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},
                  {"id":"currency-lower","kind":"card_annual_fee_currency","value_type":"currency_code","value":"usd","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},
                  {"id":"day-leading-zero","kind":"card_billing_day","value_type":"integer","value":"08","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},
                  {"id":"valid-free","kind":"card_annual_fee_amount","value_type":"decimal_string","value":"0","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}
                ]
              }
            }
        """.trimIndent()

        val facts = governedMaintenanceProfile(NodeKind.PAYMENT_INSTRUMENT, fields).facts
        assertEquals(listOf("valid-free"), facts.map { it.id })
        assertEquals("0", facts.single().value)
    }

    @Test
    fun retiredAndPausedRemainRealityButCurrentProjectionFiltersThem() {
        val fields = """
            {
              "maintenance_profile": {
                "version": 1,
                "facts": [{
                  "id": "old",
                  "kind": "card_autopay_mode",
                  "value_type": "text",
                  "value": "legacy",
                  "state": "retired",
                  "verification_basis_type": "user_confirmed",
                  "confirmed_at": "t",
                  "retired_at": "t2"
                }],
                "schedules": [{
                  "id": "paused",
                  "kind": "custom_maintenance",
                  "state": "paused",
                  "cadence": {"kind":"manual_only"},
                  "verification_basis_type": "user_confirmed",
                  "confirmed_at": "t"
                }]
              }
            }
        """.trimIndent()

        val all = governedMaintenanceProfile(NodeKind.PAYMENT_INSTRUMENT, fields)
        assertEquals(1, all.facts.size)
        assertEquals(1, all.schedules.size)
        assertTrue(currentMaintenanceFacts(NodeKind.PAYMENT_INSTRUMENT, fields).isEmpty())
        assertTrue(currentMaintenanceSchedules(NodeKind.PAYMENT_INSTRUMENT, fields).isEmpty())
    }

    @Test
    fun intervalOccurrenceBecomesOverdueButNeverCompletedByTime() {
        val schedule = ConfirmedMaintenanceSchedule(
            id = "keep",
            kind = MaintenanceScheduleKind.NUMBER_KEEP_ALIVE,
            state = MaintenanceScheduleState.ACTIVE,
            cadence = ConfirmedMaintenanceCadence(
                kind = MaintenanceCadenceKind.INTERVAL_DAYS,
                intervalDays = 90,
                anchorDate = "2026-08-07",
            ),
            verificationBasisType = com.pdig.core.generated.VerificationBasisType.USER_CONFIRMED,
            confirmedAt = "t",
            evidenceRefs = emptyList(),
            lastCompletedAt = "2026-08-07T00:00:00Z",
            retiredAt = null,
        )

        val occurrence = requireNotNull(nextMaintenanceOccurrence(schedule, "2026-11-10"))
        assertEquals("2026-11-05", occurrence.dueDate)
        assertEquals(MaintenanceOccurrenceStatus.OVERDUE, occurrence.status)
        assertTrue(occurrence.explanation.contains("保号"))
        assertEquals("2026-08-07T00:00:00Z", schedule.lastCompletedAt)
    }

    @Test
    fun monthlyOverflowPoliciesAreExplicit() {
        fun schedule(policy: com.pdig.core.generated.MaintenanceOverflowPolicy) =
            ConfirmedMaintenanceSchedule(
                id = "bill-" + policy.wire,
                kind = MaintenanceScheduleKind.CARD_BILLING_CHECKPOINT,
                state = MaintenanceScheduleState.ACTIVE,
                cadence = ConfirmedMaintenanceCadence(
                    kind = MaintenanceCadenceKind.MONTHLY_DAY,
                    dayOfMonth = 31,
                    overflowPolicy = policy,
                ),
                verificationBasisType = com.pdig.core.generated.VerificationBasisType.USER_CONFIRMED,
                confirmedAt = "t",
                evidenceRefs = emptyList(),
                lastCompletedAt = null,
                retiredAt = null,
            )

        val clamp = requireNotNull(
            nextMaintenanceOccurrence(
                schedule(com.pdig.core.generated.MaintenanceOverflowPolicy.CLAMP_TO_LAST_DAY),
                "2026-02-10",
            ),
        )
        assertEquals("2026-02-28", clamp.dueDate)
        assertEquals(MaintenanceOccurrenceStatus.UPCOMING, clamp.status)

        val skip = requireNotNull(
            nextMaintenanceOccurrence(
                schedule(com.pdig.core.generated.MaintenanceOverflowPolicy.SKIP_OCCURRENCE),
                "2026-02-10",
            ),
        )
        assertEquals("2026-03-31", skip.dueDate)

        val review = requireNotNull(
            nextMaintenanceOccurrence(
                schedule(com.pdig.core.generated.MaintenanceOverflowPolicy.USER_CONFIRM),
                "2026-02-10",
            ),
        )
        assertEquals(null, review.dueDate)
        assertEquals(MaintenanceOccurrenceStatus.NEEDS_REVIEW, review.status)
    }

    @Test
    fun pausedAndManualOnlySchedulesDoNotInventTimelineOccurrences() {
        val paused = ConfirmedMaintenanceSchedule(
            id = "paused",
            kind = MaintenanceScheduleKind.CUSTOM_MAINTENANCE,
            state = MaintenanceScheduleState.PAUSED,
            cadence = ConfirmedMaintenanceCadence(kind = MaintenanceCadenceKind.ONE_TIME, dueAt = "2026-10-10"),
            verificationBasisType = com.pdig.core.generated.VerificationBasisType.USER_CONFIRMED,
            confirmedAt = "t",
            evidenceRefs = emptyList(),
            lastCompletedAt = null,
            retiredAt = null,
        )
        val manual = paused.copy(
            id = "manual",
            state = MaintenanceScheduleState.ACTIVE,
            cadence = ConfirmedMaintenanceCadence(kind = MaintenanceCadenceKind.MANUAL_ONLY),
        )

        assertEquals(null, nextMaintenanceOccurrence(paused, "2026-10-10"))
        assertEquals(null, nextMaintenanceOccurrence(manual, "2026-10-10"))
    }

    @Test
    fun lookalikeProviderAndReferenceMetadataNeverCreateLifecycle() {
        val fields = """
            {
              "issuer": "Some Bank",
              "carrier": "Some Telco",
              "annual_fee": "100",
              "billing_day": "8",
              "keep_alive_due": "2026-11-05",
              "reference_fixture": true
            }
        """.trimIndent()

        val card = governedMaintenanceProfile(NodeKind.PAYMENT_INSTRUMENT, fields)
        val identity = governedMaintenanceProfile(NodeKind.IDENTITY_ANCHOR, fields)
        assertTrue(card.facts.isEmpty() && card.schedules.isEmpty())
        assertTrue(identity.facts.isEmpty() && identity.schedules.isEmpty())
    }
}
