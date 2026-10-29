import { describe, it, expect } from '@ohos/hypium';
import { RegionFacet } from '../main/ets/generated/CanonicalEnums';
import {
  RegionLensSelectionStatus,
  currentConfirmedRegionFacts,
  defaultRegionLensSelection,
  governedRegionFacts,
} from '../main/ets/domain/RegionFacts';

export default function regionFactsHostTest() {
  describe('regionFactsHost', () => {
    it('confirmedFactsUseGovernedPrecedence', 0, () => {
      const fields =
        '{"region_facts":{"version":1,"items":[' +
        '{"id":"service","facet":"service_market","territory_code":"GB","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},' +
        '{"id":"number","facet":"numbering_territory","territory_code":"HK","subdivision_code":"HK-HK","state":"confirmed","verification_basis_type":"authoritative_source","confirmed_at":"t2","evidence_refs":["ev-region"]}' +
        ']}}';
      const facts = governedRegionFacts(fields);
      expect(facts.length).assertEqual(2);
      expect(facts[1].facet).assertEqual(RegionFacet.NUMBERING_TERRITORY);
      expect(facts[1].territoryCode).assertEqual('HK');
      expect(facts[1].subdivisionCode).assertEqual('HK-HK');

      const lens = defaultRegionLensSelection(fields);
      expect(lens.status).assertEqual(RegionLensSelectionStatus.SELECTED);
      expect(lens.territoryCode).assertEqual('HK');
      expect(lens.facet).assertEqual(RegionFacet.NUMBERING_TERRITORY);
    });

    it('samePriorityConflictNeedsReview', 0, () => {
      const fields =
        '{"region_facts":{"version":1,"items":[' +
        '{"id":"a","facet":"service_market","territory_code":"GB","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},' +
        '{"id":"b","facet":"service_market","territory_code":"US","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}' +
        ']}}';
      const lens = defaultRegionLensSelection(fields);
      expect(lens.status).assertEqual(RegionLensSelectionStatus.NEEDS_REVIEW);
      expect(lens.facet).assertEqual(RegionFacet.SERVICE_MARKET);
      expect(lens.territoryCode === undefined).assertTrue();
    });

    it('proposalAndMalformedItemsFailClosed', 0, () => {
      const fields =
        '{"region_facts":{"version":1,"items":[' +
        '{"id":"proposal","facet":"service_market","territory_code":"GB","state":"proposal","verification_basis_type":"user_confirmed","confirmed_at":"t"},' +
        '{"id":"lower","facet":"service_market","territory_code":"gb","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},' +
        '{"id":"valid","facet":"provider_jurisdiction","territory_code":"SG","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}' +
        ']}}';
      const facts = governedRegionFacts(fields);
      expect(facts.length).assertEqual(1);
      expect(facts[0].id).assertEqual('valid');
      expect(defaultRegionLensSelection(fields).territoryCode).assertEqual('SG');
    });

    it('retiredAndPhysicalLocationStayOutOfDefaultLens', 0, () => {
      const fields =
        '{"region_facts":{"version":1,"items":[' +
        '{"id":"retired","facet":"issuance_jurisdiction","territory_code":"CN","state":"retired","verification_basis_type":"user_confirmed","confirmed_at":"t","retired_at":"t2"},' +
        '{"id":"physical","facet":"physical_location","territory_code":"US","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}' +
        ']}}';
      expect(governedRegionFacts(fields).length).assertEqual(2);
      expect(currentConfirmedRegionFacts(fields).length).assertEqual(1);
      expect(defaultRegionLensSelection(fields).status).assertEqual(
        RegionLensSelectionStatus.UNKNOWN,
      );
    });

    it('legacyLookalikeMetadataNeverInfersRegion', 0, () => {
      const fields =
        '{"currency":"GBP","issuer":"Hong Kong Bank","phone":"+852 5555","locale":"en-GB"}';
      expect(governedRegionFacts(fields).length).assertEqual(0);
      expect(defaultRegionLensSelection(fields).status).assertEqual(
        RegionLensSelectionStatus.UNKNOWN,
      );
    });

    it('invalidSubdivisionDropsOnlyThatItem', 0, () => {
      const fields =
        '{"region_facts":{"version":1,"items":[' +
        '{"id":"bad","facet":"issuance_jurisdiction","territory_code":"CN","subdivision_code":"US-CA","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},' +
        '{"id":"good","facet":"issuance_jurisdiction","territory_code":"CN","subdivision_code":"CN-SX","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}' +
        ']}}';
      const facts = governedRegionFacts(fields);
      expect(facts.length).assertEqual(1);
      expect(facts[0].subdivisionCode).assertEqual('CN-SX');
    });
  });
}
