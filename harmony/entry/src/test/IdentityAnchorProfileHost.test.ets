import { describe, it, expect } from '@ohos/hypium';
import {
  IdentityAnchorSubtype,
  NodeKind,
  VerificationBasisType,
} from '../main/ets/generated/CanonicalEnums';
import { confirmedIdentityAnchorProfile } from '../main/ets/domain/IdentityAnchorProfile';

export default function identityAnchorProfileHostTest() {
  describe('identityAnchorProfileHost', () => {
    it('confirmedProfileParsesOnlyWithGovernedAuthority', 0, () => {
      const profile = confirmedIdentityAnchorProfile(
        NodeKind.IDENTITY_ANCHOR,
        '{"identity_anchor_profile":{"version":1,"subtype":"phone_number","verification_basis_type":"user_confirmed","confirmed_at":"2026-10-10T00:00:00Z","evidence_refs":["ev-1"]}}',
      );
      expect(profile !== undefined).assertTrue();
      if (profile !== undefined) {
        expect(profile.subtype).assertEqual(IdentityAnchorSubtype.PHONE_NUMBER);
        expect(profile.verificationBasisType).assertEqual(VerificationBasisType.USER_CONFIRMED);
        expect(profile.confirmedAt).assertEqual('2026-10-10T00:00:00Z');
        expect(profile.evidenceRefs.length).assertEqual(1);
        expect(profile.evidenceRefs[0]).assertEqual('ev-1');
      }
    });

    it('bareLegacySubtypeNeverBecomesAuthority', 0, () => {
      const profile = confirmedIdentityAnchorProfile(
        NodeKind.IDENTITY_ANCHOR,
        '{"subtype":"phone_number"}',
      );
      expect(profile === undefined).assertTrue();
    });

    it('invalidOrWrongKindFailsClosed', 0, () => {
      const invalid = confirmedIdentityAnchorProfile(
        NodeKind.IDENTITY_ANCHOR,
        '{"identity_anchor_profile":{"version":1,"subtype":"sms","verification_basis_type":"user_confirmed","confirmed_at":"t"}}',
      );
      expect(invalid === undefined).assertTrue();

      const wrongKind = confirmedIdentityAnchorProfile(
        NodeKind.ACCOUNT,
        '{"identity_anchor_profile":{"version":1,"subtype":"email_address","verification_basis_type":"user_confirmed","confirmed_at":"t"}}',
      );
      expect(wrongKind === undefined).assertTrue();
    });
  });
}
