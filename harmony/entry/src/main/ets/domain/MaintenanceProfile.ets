import {
  IdentityAnchorSubtype,
  MaintenanceCadenceKind,
  MaintenanceFactKind,
  MaintenanceFactState,
  MaintenanceOverflowPolicy,
  MaintenanceScheduleKind,
  MaintenanceScheduleState,
  MaintenanceValueType,
  NodeKind,
  VerificationBasisType,
  maintenanceCadenceKindFromWire,
  maintenanceFactKindFromWire,
  maintenanceFactStateFromWire,
  maintenanceOverflowPolicyFromWire,
  maintenanceScheduleKindFromWire,
  maintenanceScheduleStateFromWire,
  maintenanceValueTypeFromWire,
  verificationBasisTypeFromWire,
} from '../generated/CanonicalEnums';
import {
  confirmedIdentityAnchorProfile,
} from './IdentityAnchorProfile';

export class ConfirmedMaintenanceFact {
  id: string = '';
  kind: MaintenanceFactKind = MaintenanceFactKind.CARD_BILLING_DAY;
  valueType: MaintenanceValueType = MaintenanceValueType.TEXT;
  value: string = '';
  state: MaintenanceFactState = MaintenanceFactState.CONFIRMED;
  verificationBasisType: VerificationBasisType = VerificationBasisType.USER_CONFIRMED;
  confirmedAt: string = '';
  evidenceRefs: string[] = [];
  validFrom?: string;
  validUntil?: string;
  retiredAt?: string;
}

export class ConfirmedMaintenanceCadence {
  kind: MaintenanceCadenceKind = MaintenanceCadenceKind.MANUAL_ONLY;
  dueAt?: string;
  dayOfMonth?: number;
  month?: number;
  day?: number;
  overflowPolicy?: MaintenanceOverflowPolicy;
  intervalDays?: number;
  anchorDate?: string;
}

export class ConfirmedMaintenanceSchedule {
  id: string = '';
  kind: MaintenanceScheduleKind = MaintenanceScheduleKind.CUSTOM_MAINTENANCE;
  state: MaintenanceScheduleState = MaintenanceScheduleState.ACTIVE;
  cadence: ConfirmedMaintenanceCadence = new ConfirmedMaintenanceCadence();
  verificationBasisType: VerificationBasisType = VerificationBasisType.USER_CONFIRMED;
  confirmedAt: string = '';
  evidenceRefs: string[] = [];
  lastCompletedAt?: string;
  retiredAt?: string;
}

export class ConfirmedMaintenanceProfile {
  facts: ConfirmedMaintenanceFact[] = [];
  schedules: ConfirmedMaintenanceSchedule[] = [];
}

export function governedMaintenanceProfile(
  kind: NodeKind,
  fieldsJson: string,
): ConfirmedMaintenanceProfile {
  const out = new ConfirmedMaintenanceProfile();

  let parsed: Object;
  try {
    parsed = JSON.parse(fieldsJson) as Object;
  } catch (e) {
    return out;
  }
  if (parsed === null || typeof parsed !== 'object' || parsed instanceof Array) {
    return out;
  }

  const root = parsed as Record<string, Object>;
  const rawContainer: Object = root['maintenance_profile'];
  if (
    rawContainer === undefined ||
    rawContainer === null ||
    typeof rawContainer !== 'object' ||
    rawContainer instanceof Array
  ) {
    return out;
  }
  const container = rawContainer as Record<string, Object>;
  const version: Object = container['version'];
  if (typeof version !== 'number' || (version as number) !== 1) {
    return out;
  }

  const phoneAuthority =
    kind === NodeKind.IDENTITY_ANCHOR &&
    confirmedIdentityAnchorProfile(kind, fieldsJson)?.subtype ===
      IdentityAnchorSubtype.PHONE_NUMBER;

  const rawFacts: Object = container['facts'];
  if (rawFacts instanceof Array) {
    for (const raw of rawFacts as Array<Object>) {
      if (
        raw === null ||
        typeof raw !== 'object' ||
        raw instanceof Array
      ) {
        continue;
      }
      const fact = decodeFact(
        kind,
        phoneAuthority,
        raw as Record<string, Object>,
      );
      if (fact !== undefined) {
        out.facts.push(fact);
      }
    }
  }

  const rawSchedules: Object = container['schedules'];
  if (rawSchedules instanceof Array) {
    for (const raw of rawSchedules as Array<Object>) {
      if (
        raw === null ||
        typeof raw !== 'object' ||
        raw instanceof Array
      ) {
        continue;
      }
      const schedule = decodeSchedule(
        kind,
        phoneAuthority,
        raw as Record<string, Object>,
      );
      if (schedule !== undefined) {
        out.schedules.push(schedule);
      }
    }
  }

  return out;
}

export function currentMaintenanceFacts(
  kind: NodeKind,
  fieldsJson: string,
): ConfirmedMaintenanceFact[] {
  return governedMaintenanceProfile(kind, fieldsJson).facts.filter(
    (item: ConfirmedMaintenanceFact) =>
      item.state === MaintenanceFactState.CONFIRMED,
  );
}

export function currentMaintenanceSchedules(
  kind: NodeKind,
  fieldsJson: string,
): ConfirmedMaintenanceSchedule[] {
  return governedMaintenanceProfile(kind, fieldsJson).schedules.filter(
    (item: ConfirmedMaintenanceSchedule) =>
      item.state === MaintenanceScheduleState.ACTIVE ||
      item.state === MaintenanceScheduleState.NEEDS_REVIEW,
  );
}

function decodeFact(
  nodeKind: NodeKind,
  phoneAuthority: boolean,
  item: Record<string, Object>,
): ConfirmedMaintenanceFact | undefined {
  const id = requiredText(item, 'id');
  if (id === undefined || !validBoundedText(id, 160)) {
    return undefined;
  }

  const rawKind: Object = item['kind'];
  if (typeof rawKind !== 'string') {
    return undefined;
  }
  const factKind = maintenanceFactKindFromWire(rawKind as string);
  if (
    factKind === undefined ||
    !factApplicable(nodeKind, phoneAuthority, factKind)
  ) {
    return undefined;
  }

  const rawValueType: Object = item['value_type'];
  if (typeof rawValueType !== 'string') {
    return undefined;
  }
  const valueType = maintenanceValueTypeFromWire(rawValueType as string);
  const value = requiredText(item, 'value');
  if (
    valueType === undefined ||
    value === undefined ||
    !validFactValue(factKind, valueType, value)
  ) {
    return undefined;
  }

  const rawState: Object = item['state'];
  const rawBasis: Object = item['verification_basis_type'];
  if (typeof rawState !== 'string' || typeof rawBasis !== 'string') {
    return undefined;
  }
  const state = maintenanceFactStateFromWire(rawState as string);
  const basis = verificationBasisTypeFromWire(rawBasis as string);
  const confirmedAt = requiredText(item, 'confirmed_at');
  if (
    state === undefined ||
    basis === undefined ||
    confirmedAt === undefined ||
    !validBoundedText(confirmedAt, 96)
  ) {
    return undefined;
  }

  const evidenceRefs = stringArray(item, 'evidence_refs');
  if (
    evidenceRefs === undefined ||
    evidenceRefs.some((value: string) => !validBoundedText(value, 512))
  ) {
    return undefined;
  }

  const validFrom = strictOptionalText(item, 'valid_from');
  const validUntil = strictOptionalText(item, 'valid_until');
  const retiredAt = strictOptionalText(item, 'retired_at');
  if (validFrom.invalid || validUntil.invalid || retiredAt.invalid) {
    return undefined;
  }

  const out = new ConfirmedMaintenanceFact();
  out.id = id;
  out.kind = factKind;
  out.valueType = valueType;
  out.value = value;
  out.state = state;
  out.verificationBasisType = basis;
  out.confirmedAt = confirmedAt;
  out.evidenceRefs = evidenceRefs;
  out.validFrom = validFrom.value;
  out.validUntil = validUntil.value;
  out.retiredAt = retiredAt.value;
  return out;
}

function decodeSchedule(
  nodeKind: NodeKind,
  phoneAuthority: boolean,
  item: Record<string, Object>,
): ConfirmedMaintenanceSchedule | undefined {
  const id = requiredText(item, 'id');
  if (id === undefined || !validBoundedText(id, 160)) {
    return undefined;
  }

  const rawKind: Object = item['kind'];
  const rawState: Object = item['state'];
  if (typeof rawKind !== 'string' || typeof rawState !== 'string') {
    return undefined;
  }
  const scheduleKind = maintenanceScheduleKindFromWire(rawKind as string);
  const state = maintenanceScheduleStateFromWire(rawState as string);
  if (
    scheduleKind === undefined ||
    state === undefined ||
    !scheduleApplicable(nodeKind, phoneAuthority, scheduleKind)
  ) {
    return undefined;
  }

  const rawCadence: Object = item['cadence'];
  if (
    rawCadence === undefined ||
    rawCadence === null ||
    typeof rawCadence !== 'object' ||
    rawCadence instanceof Array
  ) {
    return undefined;
  }
  const cadence = decodeCadence(rawCadence as Record<string, Object>);
  if (
    cadence === undefined ||
    !cadenceApplicable(scheduleKind, cadence.kind)
  ) {
    return undefined;
  }

  const rawBasis: Object = item['verification_basis_type'];
  if (typeof rawBasis !== 'string') {
    return undefined;
  }
  const basis = verificationBasisTypeFromWire(rawBasis as string);
  const confirmedAt = requiredText(item, 'confirmed_at');
  if (
    basis === undefined ||
    confirmedAt === undefined ||
    !validBoundedText(confirmedAt, 96)
  ) {
    return undefined;
  }

  const evidenceRefs = stringArray(item, 'evidence_refs');
  if (
    evidenceRefs === undefined ||
    evidenceRefs.some((value: string) => !validBoundedText(value, 512))
  ) {
    return undefined;
  }

  const lastCompletedAt = strictOptionalText(item, 'last_completed_at');
  const retiredAt = strictOptionalText(item, 'retired_at');
  if (lastCompletedAt.invalid || retiredAt.invalid) {
    return undefined;
  }

  const out = new ConfirmedMaintenanceSchedule();
  out.id = id;
  out.kind = scheduleKind;
  out.state = state;
  out.cadence = cadence;
  out.verificationBasisType = basis;
  out.confirmedAt = confirmedAt;
  out.evidenceRefs = evidenceRefs;
  out.lastCompletedAt = lastCompletedAt.value;
  out.retiredAt = retiredAt.value;
  return out;
}

function decodeCadence(
  obj: Record<string, Object>,
): ConfirmedMaintenanceCadence | undefined {
  const rawKind: Object = obj['kind'];
  if (typeof rawKind !== 'string') {
    return undefined;
  }
  const kind = maintenanceCadenceKindFromWire(rawKind as string);
  if (kind === undefined) {
    return undefined;
  }

  const out = new ConfirmedMaintenanceCadence();
  out.kind = kind;

  if (kind === MaintenanceCadenceKind.ONE_TIME) {
    const dueAt = requiredText(obj, 'due_at');
    if (dueAt === undefined || !validBoundedText(dueAt, 96)) {
      return undefined;
    }
    out.dueAt = dueAt;
    return out;
  }

  if (kind === MaintenanceCadenceKind.MONTHLY_DAY) {
    const day = intField(obj, 'day_of_month');
    const overflow = overflowPolicy(obj);
    if (
      day === undefined ||
      day < 1 ||
      day > 31 ||
      overflow === undefined
    ) {
      return undefined;
    }
    out.dayOfMonth = day;
    out.overflowPolicy = overflow;
    return out;
  }

  if (kind === MaintenanceCadenceKind.YEARLY_MONTH_DAY) {
    const month = intField(obj, 'month');
    const day = intField(obj, 'day');
    const overflow = overflowPolicy(obj);
    if (
      month === undefined ||
      month < 1 ||
      month > 12 ||
      day === undefined ||
      day < 1 ||
      day > 31 ||
      overflow === undefined
    ) {
      return undefined;
    }
    out.month = month;
    out.day = day;
    out.overflowPolicy = overflow;
    return out;
  }

  if (kind === MaintenanceCadenceKind.INTERVAL_DAYS) {
    const interval = intField(obj, 'interval_days');
    const anchor = requiredText(obj, 'anchor_date');
    if (
      interval === undefined ||
      interval < 1 ||
      interval > 3660 ||
      anchor === undefined ||
      !dateOnly(anchor)
    ) {
      return undefined;
    }
    out.intervalDays = interval;
    out.anchorDate = anchor;
    return out;
  }

  return out;
}

function factApplicable(
  nodeKind: NodeKind,
  phoneAuthority: boolean,
  kind: MaintenanceFactKind,
): boolean {
  if (
    kind === MaintenanceFactKind.CARD_ANNUAL_FEE_AMOUNT ||
    kind === MaintenanceFactKind.CARD_ANNUAL_FEE_CURRENCY ||
    kind === MaintenanceFactKind.CARD_BILLING_DAY ||
    kind === MaintenanceFactKind.CARD_PAYMENT_DUE_DAY ||
    kind === MaintenanceFactKind.CARD_AUTOPAY_MODE
  ) {
    return nodeKind === NodeKind.PAYMENT_INSTRUMENT;
  }
  return phoneAuthority;
}

function scheduleApplicable(
  nodeKind: NodeKind,
  phoneAuthority: boolean,
  kind: MaintenanceScheduleKind,
): boolean {
  if (
    kind === MaintenanceScheduleKind.CARD_ANNUAL_FEE_CHECKPOINT ||
    kind === MaintenanceScheduleKind.CARD_BILLING_CHECKPOINT ||
    kind === MaintenanceScheduleKind.CARD_PAYMENT_DUE_CHECKPOINT
  ) {
    return nodeKind === NodeKind.PAYMENT_INSTRUMENT;
  }
  if (
    kind === MaintenanceScheduleKind.NUMBER_KEEP_ALIVE ||
    kind === MaintenanceScheduleKind.NUMBER_PLAN_RENEWAL
  ) {
    return phoneAuthority;
  }
  return true;
}

function cadenceApplicable(
  scheduleKind: MaintenanceScheduleKind,
  cadenceKind: MaintenanceCadenceKind,
): boolean {
  if (scheduleKind === MaintenanceScheduleKind.CARD_ANNUAL_FEE_CHECKPOINT) {
    return (
      cadenceKind === MaintenanceCadenceKind.ONE_TIME ||
      cadenceKind === MaintenanceCadenceKind.YEARLY_MONTH_DAY
    );
  }
  if (
    scheduleKind === MaintenanceScheduleKind.CARD_BILLING_CHECKPOINT ||
    scheduleKind === MaintenanceScheduleKind.CARD_PAYMENT_DUE_CHECKPOINT
  ) {
    return cadenceKind === MaintenanceCadenceKind.MONTHLY_DAY;
  }
  if (scheduleKind === MaintenanceScheduleKind.NUMBER_KEEP_ALIVE) {
    return (
      cadenceKind === MaintenanceCadenceKind.ONE_TIME ||
      cadenceKind === MaintenanceCadenceKind.INTERVAL_DAYS ||
      cadenceKind === MaintenanceCadenceKind.MANUAL_ONLY
    );
  }
  if (scheduleKind === MaintenanceScheduleKind.NUMBER_PLAN_RENEWAL) {
    return (
      cadenceKind === MaintenanceCadenceKind.ONE_TIME ||
      cadenceKind === MaintenanceCadenceKind.MONTHLY_DAY ||
      cadenceKind === MaintenanceCadenceKind.INTERVAL_DAYS
    );
  }
  if (scheduleKind === MaintenanceScheduleKind.FACT_FRESHNESS_REVIEW) {
    return (
      cadenceKind === MaintenanceCadenceKind.INTERVAL_DAYS ||
      cadenceKind === MaintenanceCadenceKind.MANUAL_ONLY
    );
  }
  return true;
}

function validFactValue(
  kind: MaintenanceFactKind,
  valueType: MaintenanceValueType,
  value: string,
): boolean {
  if (
    kind === MaintenanceFactKind.CARD_ANNUAL_FEE_AMOUNT ||
    kind === MaintenanceFactKind.NUMBER_PLAN_COST
  ) {
    return (
      valueType === MaintenanceValueType.DECIMAL_STRING &&
      decimalString(value)
    );
  }
  if (
    kind === MaintenanceFactKind.CARD_ANNUAL_FEE_CURRENCY ||
    kind === MaintenanceFactKind.NUMBER_PLAN_CURRENCY
  ) {
    return (
      valueType === MaintenanceValueType.CURRENCY_CODE &&
      currencyCode(value)
    );
  }
  if (
    kind === MaintenanceFactKind.CARD_BILLING_DAY ||
    kind === MaintenanceFactKind.CARD_PAYMENT_DUE_DAY
  ) {
    if (valueType !== MaintenanceValueType.INTEGER) {
      return false;
    }
    const parsed = Number(value);
    return (
      Number.isInteger(parsed) &&
      parsed >= 1 &&
      parsed <= 31 &&
      parsed.toString() === value
    );
  }
  return (
    valueType === MaintenanceValueType.TEXT &&
    validBoundedText(value, 240)
  );
}

function overflowPolicy(
  obj: Record<string, Object>,
): MaintenanceOverflowPolicy | undefined {
  const raw: Object = obj['overflow_policy'];
  if (typeof raw !== 'string') {
    return undefined;
  }
  return maintenanceOverflowPolicyFromWire(raw as string);
}

function intField(
  obj: Record<string, Object>,
  key: string,
): number | undefined {
  const raw: Object = obj[key];
  if (typeof raw !== 'number' || !Number.isInteger(raw as number)) {
    return undefined;
  }
  return raw as number;
}

class OptionalTextResult {
  invalid: boolean = false;
  value?: string;
}

function strictOptionalText(
  obj: Record<string, Object>,
  key: string,
): OptionalTextResult {
  const out = new OptionalTextResult();
  const raw: Object = obj[key];
  if (raw === undefined) {
    return out;
  }
  if (typeof raw !== 'string') {
    out.invalid = true;
    return out;
  }
  const value = (raw as string).trim();
  if (!validBoundedText(value, 128)) {
    out.invalid = true;
    return out;
  }
  out.value = value;
  return out;
}

function requiredText(
  obj: Record<string, Object>,
  key: string,
): string | undefined {
  const raw: Object = obj[key];
  if (typeof raw !== 'string') {
    return undefined;
  }
  const value = (raw as string).trim();
  return value.length > 0 ? value : undefined;
}

function stringArray(
  obj: Record<string, Object>,
  key: string,
): string[] | undefined {
  const raw: Object = obj[key];
  if (raw === undefined) {
    return [];
  }
  if (!(raw instanceof Array)) {
    return undefined;
  }
  const out: string[] = [];
  for (const item of raw as Array<Object>) {
    if (typeof item !== 'string') {
      return undefined;
    }
    out.push(item as string);
  }
  return out;
}

function validBoundedText(value: string, max: number): boolean {
  if (value.length === 0 || value.length > max) {
    return false;
  }
  for (let i = 0; i < value.length; i++) {
    const code = value.charCodeAt(i);
    if (code < 0x20 || code === 0x7F) {
      return false;
    }
  }
  return true;
}

function decimalString(value: string): boolean {
  return /^(0|[1-9][0-9]*)(\.[0-9]+)?$/.test(value);
}

function currencyCode(value: string): boolean {
  return /^[A-Z]{3}$/.test(value);
}

function dateOnly(value: string): boolean {
  return /^[0-9]{4}-[0-9]{2}-[0-9]{2}$/.test(value);
}
