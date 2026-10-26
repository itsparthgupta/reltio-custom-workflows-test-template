package com.company.workflow.location;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.Utility;
import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.actions.WorkflowAction;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.api.rest.ReltioException;

/**
 * Reads the Location entity's {@code Country} (from the DCR or live entity) and sets
 * {@link RegionalReviewerRouting#VARIABLE_REGIONAL_GROUP} for the first review step.
 */
public class ResolveRegionalReviewerGroup implements WorkflowAction {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern CHANGE_REQUEST_URI_PATTERN =
            Pattern.compile("^(changeRequests/[A-Za-z0-9]*).*");
    private static final String LOCATION_TYPE = "configuration/entityTypes/Location";
    private static final String COUNTRY_ATTRIBUTE = "Country";
    private static final String ISO2_ATTRIBUTE = "ISO3166-2";

    @WorkflowService
    ReltioApi reltioApi;

    @WorkflowService
    ExecutionService executionService;

    @Override
    public void execute(Execution execution) throws Exception {
        String base = Utility.generateTenantUrl(execution.getEnvironmentUrl(), execution.getTenantId()) + "/";
        String token = execution.getAccessToken();

        String dcrUri = findChangeRequestUri(execution.getObjectUris());
        if (dcrUri == null) {
            throw new ReltioException("No change request found for objectUris = " + execution.getObjectUris());
        }

        Map<String, Object> dcr = MAPPER.readValue(
                reltioApi.invokeApi(token, base + dcrUri, "GET", ""), Map.class);

        String country = resolveCountryFromDcr(dcr, base, token);
        String group = RegionalReviewerRouting.reviewerGroupForCountry(country);
        executionService.setVariable(execution.getId(), RegionalReviewerRouting.VARIABLE_REGIONAL_GROUP, group);
    }

    @SuppressWarnings("unchecked")
    private String resolveCountryFromDcr(Map<String, Object> dcr, String base, String token) throws Exception {
        Map<String, List<Map<String, Object>>> changes =
                (Map<String, List<Map<String, Object>>>) dcr.get("changes");
        if (changes == null || changes.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : changes.entrySet()) {
            if (!entry.getKey().startsWith("entities/")) {
                continue;
            }
            String entityUri = entry.getKey();
            String entityType = resolveEntityType(entry.getValue(), entityUri, base, token);
            if (!LOCATION_TYPE.equals(entityType)) {
                continue;
            }

            String fromChanges = countryFromChanges(entry.getValue());
            if (fromChanges != null) {
                return fromChanges;
            }

            Map<String, Object> entity = MAPPER.readValue(
                    reltioApi.invokeApi(token, base + entityUri, "GET", ""), Map.class);
            return readCountryFromEntity(entity);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String resolveEntityType(List<Map<String, Object>> entityChanges, String entityUri,
            String base, String token) throws Exception {
        for (Map<String, Object> change : entityChanges) {
            if ("CREATE_ENTITY".equals(change.get("type"))) {
                Object newValue = change.get("newValue");
                if (newValue instanceof Map) {
                    return (String) ((Map<?, ?>) newValue).get("type");
                }
            }
        }
        Map<String, Object> entity = MAPPER.readValue(
                reltioApi.invokeApi(token, base + entityUri, "GET", ""), Map.class);
        return (String) entity.get("type");
    }

    private static String countryFromChanges(List<Map<String, Object>> entityChanges) {
        String country = null;
        for (Map<String, Object> change : entityChanges) {
            String type = (String) change.get("type");
            if ("CREATE_ENTITY".equals(type)) {
                country = countryFromNewEntity(change.get("newValue"));
            } else if (isCountryAttributeChange(change)) {
                country = stringValue(change.get("newValue"));
            }
        }
        return country;
    }

    @SuppressWarnings("unchecked")
    private static String countryFromNewEntity(Object newValue) {
        if (!(newValue instanceof Map)) {
            return null;
        }
        Map<String, Object> entity = (Map<String, Object>) newValue;
        return readCountryFromEntity(entity);
    }

    @SuppressWarnings("unchecked")
    private static String readCountryFromEntity(Map<String, Object> entity) {
        Object attributesObj = entity.get("attributes");
        if (!(attributesObj instanceof Map)) {
            return null;
        }
        Map<String, Object> attributes = (Map<String, Object>) attributesObj;
        String country = firstAttributeValue(attributes, COUNTRY_ATTRIBUTE);
        if (country != null) {
            return country;
        }
        return firstAttributeValue(attributes, ISO2_ATTRIBUTE);
    }

    @SuppressWarnings("unchecked")
    private static String firstAttributeValue(Map<String, Object> attributes, String name) {
        Object valuesObj = attributes.get(name);
        if (!(valuesObj instanceof List)) {
            return null;
        }
        List<Object> values = (List<Object>) valuesObj;
        if (values.isEmpty()) {
            return null;
        }
        Object first = values.get(0);
        if (first instanceof Map) {
            return stringValue(((Map<?, ?>) first).get("value"));
        }
        return stringValue(first);
    }

    private static boolean isCountryAttributeChange(Map<String, Object> change) {
        Object path = change.get("path");
        if (path == null) {
            return false;
        }
        String pathStr = path.toString();
        return pathStr.endsWith("/" + COUNTRY_ATTRIBUTE)
                || pathStr.endsWith("/attributes/" + COUNTRY_ATTRIBUTE)
                || COUNTRY_ATTRIBUTE.equals(pathStr);
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String findChangeRequestUri(List<String> objectUris) {
        for (String objectUri : objectUris) {
            Matcher matcher = CHANGE_REQUEST_URI_PATTERN.matcher(objectUri);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
