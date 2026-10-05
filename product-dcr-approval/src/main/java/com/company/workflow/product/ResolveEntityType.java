package com.company.workflow.product;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.Utility;
import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.actions.WorkflowAction;
import com.reltio.workflow.api.rest.ReltioApi;

/**
 * Resolves affected entity type URIs from the DCR and sets {@code entityType} for routing.
 */
public class ResolveEntityType implements WorkflowAction {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern CHANGE_REQUEST_URI_PATTERN =
            Pattern.compile("^(changeRequests/[A-Za-z0-9]*).*");

    static final String ENTITY_TYPE_VARIABLE = "entityType";

    @WorkflowService
    ReltioApi reltioApi;

    @WorkflowService
    ExecutionService executionService;

    @Override
    public void execute(Execution execution) throws Exception {
        String base = Utility.generateTenantUrl(execution.getEnvironmentUrl(), execution.getTenantId()) + "/";
        String token = execution.getAccessToken();

        String dcrUri = findChangeRequestUri(execution.getObjectUris());
        Set<String> types = new HashSet<>();
        if (dcrUri != null) {
            Map<String, Object> dcr = MAPPER.readValue(
                    reltioApi.invokeApi(token, base + dcrUri, "GET", ""), Map.class);
            Map<String, List<Map<String, Object>>> changes =
                    (Map<String, List<Map<String, Object>>>) dcr.get("changes");
            if (changes != null) {
                for (Map.Entry<String, List<Map<String, Object>>> entry : changes.entrySet()) {
                    String type = null;
                    for (Map<String, Object> change : entry.getValue()) {
                        if ("CREATE_ENTITY".equals(change.get("type"))) {
                            Object newValue = change.get("newValue");
                            if (newValue instanceof Map) {
                                type = (String) ((Map<?, ?>) newValue).get("type");
                            }
                        }
                    }
                    if (type == null && entry.getKey().startsWith("entities/")) {
                        Map<String, Object> entity = MAPPER.readValue(
                                reltioApi.invokeApi(token, base + entry.getKey(), "GET", ""), Map.class);
                        type = (String) entity.get("type");
                    }
                    if (type != null) {
                        types.add(type);
                    }
                }
            }
        }

        String resolved = types.size() == 1 ? types.iterator().next()
                : types.isEmpty() ? "UNKNOWN" : "MIXED";
        executionService.setVariable(execution.getId(), ENTITY_TYPE_VARIABLE, resolved);
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
