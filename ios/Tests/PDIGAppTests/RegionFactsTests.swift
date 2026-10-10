import XCTest
import PDIGCore

final class RegionFactsTests: XCTestCase {
    func testConfirmedFactsDecodeAndDefaultLensUsesGovernedPrecedence() {
        let fields = #"""
        {"region_facts":{"version":1,"items":[
          {"id":"service","facet":"service_market","territory_code":"GB","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},
          {"id":"number","facet":"numbering_territory","territory_code":"HK","subdivision_code":"HK-HK","state":"confirmed","verification_basis_type":"authoritative_source","confirmed_at":"t2","evidence_refs":["ev-region"]}
        ]}}
        """#

        let facts = governedRegionFacts(fieldsJson: fields)
        XCTAssertEqual(facts.count, 2)
        XCTAssertEqual(facts[1].facet, .numberingTerritory)
        XCTAssertEqual(facts[1].territoryCode, "HK")
        XCTAssertEqual(facts[1].subdivisionCode, "HK-HK")
        XCTAssertEqual(facts[1].verificationBasisType, .authoritativeSource)

        let lens = defaultRegionLensSelection(fieldsJson: fields)
        XCTAssertEqual(lens.status, .selected)
        XCTAssertEqual(lens.territoryCode, "HK")
        XCTAssertEqual(lens.facet, .numberingTerritory)
    }

    func testSamePriorityConflictNeedsReview() {
        let fields = #"""
        {"region_facts":{"version":1,"items":[
          {"id":"a","facet":"service_market","territory_code":"GB","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},
          {"id":"b","facet":"service_market","territory_code":"US","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}
        ]}}
        """#

        let lens = defaultRegionLensSelection(fieldsJson: fields)
        XCTAssertEqual(lens.status, .needsReview)
        XCTAssertEqual(lens.facet, .serviceMarket)
        XCTAssertNil(lens.territoryCode)
    }

    func testMalformedAndProposalItemsFailClosedWithoutErasingValidSibling() {
        let fields = #"""
        {"region_facts":{"version":1,"items":[
          {"id":"proposal","facet":"service_market","territory_code":"GB","state":"proposal","verification_basis_type":"user_confirmed","confirmed_at":"t"},
          {"id":"bad-code","facet":"service_market","territory_code":"gb","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},
          {"id":"valid","facet":"provider_jurisdiction","territory_code":"SG","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}
        ]}}
        """#

        let facts = governedRegionFacts(fieldsJson: fields)
        XCTAssertEqual(facts.map(\.id), ["valid"])
        XCTAssertEqual(defaultRegionLensSelection(fieldsJson: fields).territoryCode, "SG")
    }

    func testRetiredAndPhysicalLocationAreExcludedFromDefaultLens() {
        let fields = #"""
        {"region_facts":{"version":1,"items":[
          {"id":"retired","facet":"issuance_jurisdiction","territory_code":"CN","state":"retired","verification_basis_type":"user_confirmed","confirmed_at":"t","retired_at":"t2"},
          {"id":"physical","facet":"physical_location","territory_code":"US","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}
        ]}}
        """#

        XCTAssertEqual(governedRegionFacts(fieldsJson: fields).count, 2)
        XCTAssertEqual(currentConfirmedRegionFacts(fieldsJson: fields).map(\.id), ["physical"])
        XCTAssertEqual(defaultRegionLensSelection(fieldsJson: fields).status, .unknown)
    }

    func testLegacyLookalikeMetadataDoesNotInferRegion() {
        let fields = #"""
        {"currency":"GBP","issuer":"Hong Kong Bank","phone":"+852 5555","locale":"en-GB","timezone":"Europe/London"}
        """#

        XCTAssertTrue(governedRegionFacts(fieldsJson: fields).isEmpty)
        XCTAssertEqual(defaultRegionLensSelection(fieldsJson: fields).status, .unknown)
    }

    func testInvalidSubdivisionDropsOnlyThatItem() {
        let fields = #"""
        {"region_facts":{"version":1,"items":[
          {"id":"bad","facet":"issuance_jurisdiction","territory_code":"CN","subdivision_code":"US-CA","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},
          {"id":"good","facet":"issuance_jurisdiction","territory_code":"CN","subdivision_code":"CN-SX","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}
        ]}}
        """#

        let facts = governedRegionFacts(fieldsJson: fields)
        XCTAssertEqual(facts.count, 1)
        XCTAssertEqual(facts.first?.subdivisionCode, "CN-SX")
    }
}
