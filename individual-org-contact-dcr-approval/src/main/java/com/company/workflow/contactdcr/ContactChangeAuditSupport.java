package com.company.workflow.contactdcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helpers for detecting email/address DCR changes and appending approval audit rows on an entity.
 */
public final class ContactChangeAuditSupport {

    static final String ENTITY_TYPE_VARIABLE = "entityType";
    static final String CONTACT_TRIPLE_REVIEW_VARIABLE = "contactChangeTripleReview";
    static final String AUDIT_ATTRIBUTE = "DcrApprovalAuditTrail";

    static final String INDIVIDUAL_TYPE = "configuration/entityTypes/Individual";
    static final String ORGANIZATION_TYPE = "configuration/entityTypes/Organization";

    private static final Pattern CHANGE_REQUEST_URI_PATTERN =
            Pattern.compile("^(changeRequests/[A-Za-z0-9]*).*");

    private static final String EMAIL_FRAGMENT = "/attributes/Email";
    private static final String ADDRESSES_FRAGMENT = "/attributes/Addresses";
    private static final String ADDRESS_FRAGMENT = "/attributes/Address";

    private ContactChangeAuditSupport() {
    }

    static String findChangeRequestUri(List<String> objectUris) {
        if (objectUris == null) {
            return null;
        }
        for (String objectUri : objectUris) {
            var matcher = CHANGE_REQUEST_URI_PATTERN.matcher(objectUri);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    static String findPrimaryEntityUri(List<String> objectUris) {
        if (objectUris == null) {
            return null;
        }
        for (String objectUri : objectUris) {
            if (objectUri != null && objectUri.startsWith("entities/")) {
                return objectUri;
            }
        }
        return null;
    }

  @SuppressWarnings("unchecked")
    static ResolvedContext resolveFromDcrMap(Map<String, Object> dcr) {
        Set<String> types = new HashSet<>();
        boolean contactChange = false;

        Map<String, List<Map<String, Object>>> changes =
                (Map<String, List<Map<String, Object>>>) dcr.get("changes");
        if (changes != null) {
            for (Map.Entry<String, List<Map<String, Object>>> entry : changes.entrySet()) {
                String entityKey = entry.getKey();
                String typeFromCreate = null;
                for (Map<String, Object> change : entry.getValue()) {
                    if (change == null) {
                        continue;
                    }
                    if ("CREATE_ENTITY".equals(change.get("type"))) {
                        Object newValue = change.get("newValue");
                        if (newValue instanceof Map) {
                            typeFromCreate = (String) ((Map<?, ?>) newValue).get("type");
                        }
                    }
                    if (isContactAttributeChange(change)) {
                        contactChange = true;
                    }
                }
                if (typeFromCreate != null) {
                    types.add(typeFromCreate);
                } else if (entityKey.startsWith("entities/")) {
                    types.add("FETCH:" + entityKey);
                }
            }
        }

        String resolvedType;
        if (types.size() == 1) {
            String only = types.iterator().next();
            resolvedType = only.startsWith("FETCH:") ? only.substring("FETCH:".length()) : only;
        } else if (types.isEmpty()) {
            resolvedType = "UNKNOWN";
        } else {
            resolvedType = "MIXED";
        }

        boolean partyType = INDIVIDUAL_TYPE.equals(resolvedType) || ORGANIZATION_TYPE.equals(resolvedType);
        boolean tripleReview = partyType && contactChange && !"MIXED".equals(resolvedType);

        return new ResolvedContext(resolvedType, tripleReview, contactChange);
    }

    static boolean isContactAttributeChange(Map<String, Object> change) {
        if (change == null) {
            return false;
        }
        for (String key : List.of("attributeType", "attribute", "attributeUri")) {
            Object value = change.get(key);
            if (value instanceof String && touchesContactAttribute((String) value)) {
                return true;
            }
        }
        Object nested = change.get("newValue");
        if (nested instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) nested;
            for (Object v : map.values()) {
                if (v instanceof String && touchesContactAttribute((String) v)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean touchesContactAttribute(String attributePath) {
        if (attributePath == null) {
            return false;
        }
        return attributePath.contains(EMAIL_FRAGMENT)
                || attributePath.contains(ADDRESSES_FRAGMENT)
                || attributePath.contains(ADDRESS_FRAGMENT);
    }

    static String buildAuditPatchBodyFromPending(
            Map<String, Object> existingEntity, List<Map<String, String>> pendingRows) throws Exception {
        List<Map<String, Object>> mergedRows = readExistingAuditRows(existingEntity);
        for (Map<String, String> pending : pendingRows) {
            Map<String, Object> row = Map.of(
                    "value", Map.of(
                            "Step", List.of(Map.of("value", nullSafe(pending.get("step")))),
                            "Decision", List.of(Map.of("value", nullSafe(pending.get("decision")))),
                            "Reviewer", List.of(Map.of("value", nullSafe(pending.get("reviewer")))),
                            "Comment", List.of(Map.of("value", nullSafe(pending.get("comment")))),
                            "RecordedAt", List.of(Map.of("value", nullSafe(pending.get("recordedAt"))))));
            mergedRows.add(row);
        }

        Map<String, Object> patch = Map.of(
                "attributes", Map.of(AUDIT_ATTRIBUTE, mergedRows));
        return new ObjectMapper().writeValueAsString(patch);
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> readExistingAuditRows(Map<String, Object> existingEntity) {
        if (existingEntity == null) {
            return new ArrayList<>();
        }
        Object attributes = existingEntity.get("attributes");
        if (!(attributes instanceof Map)) {
            return new ArrayList<>();
        }
        Map<?, ?> attributeMap = (Map<?, ?>) attributes;
        Object audit = attributeMap.get(AUDIT_ATTRIBUTE);
        if (!(audit instanceof Collection)) {
            return new ArrayList<>();
        }
        Collection<?> collection = (Collection<?>) audit;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof Map) {
                rows.add((Map<String, Object>) item);
            }
        }
        return rows;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    static final class ResolvedContext {
        private final String entityType;
        private final boolean contactChangeTripleReview;
        private final boolean contactChange;

        ResolvedContext(String entityType, boolean contactChangeTripleReview, boolean contactChange) {
            this.entityType = entityType;
            this.contactChangeTripleReview = contactChangeTripleReview;
            this.contactChange = contactChange;
        }

        String entityType() {
            return entityType;
        }

        boolean contactChangeTripleReview() {
            return contactChangeTripleReview;
        }

        boolean contactChange() {
            return contactChange;
        }
    }
}