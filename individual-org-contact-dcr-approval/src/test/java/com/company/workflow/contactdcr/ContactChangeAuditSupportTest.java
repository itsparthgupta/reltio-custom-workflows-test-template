package com.company.workflow.contactdcr;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ContactChangeAuditSupportTest {

    @Test
    void detectsEmailChangeForIndividual() {
        Map<String, Object> dcr = Map.of(
                "changes", Map.of(
                        "entities/abc", List.of(Map.of(
                                "type", "UPDATE_ATTRIBUTE",
                                "attributeType",
                                "configuration/entityTypes/Individual/attributes/Email/attributes/Email"))));

        var resolved = ContactChangeAuditSupport.resolveFromDcrMap(dcr);
        assertTrue(resolved.contactChange());
    }

    @Test
    void tripleReviewRequiresPartyTypeAndContactChange() {
        Map<String, Object> dcr = Map.of(
                "changes", Map.of(
                        "entities/abc", List.of(Map.of(
                                "type", "UPDATE_ATTRIBUTE",
                                "attributeType",
                                "configuration/entityTypes/Organization/attributes/Addresses/attributes/City"))));

        var resolved = new ContactChangeAuditSupport.ResolvedContext(
                ContactChangeAuditSupport.ORGANIZATION_TYPE, true, true);
        assertTrue(resolved.contactChangeTripleReview());
    }

    @Test
    void ignoresUnrelatedAttributeChange() {
        Map<String, Object> dcr = Map.of(
                "changes", Map.of(
                        "entities/abc", List.of(Map.of(
                                "type", "UPDATE_ATTRIBUTE",
                                "attributeType",
                                "configuration/entityTypes/Individual/attributes/Prefix"))));
        var resolved = ContactChangeAuditSupport.resolveFromDcrMap(dcr);
        assertFalse(resolved.contactChange());
    }
}