package com.company.workflow.contactdcr;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.Utility;
import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.actions.WorkflowAction;
import com.reltio.workflow.api.rest.ReltioApi;

/**
 * Resolves entity type and whether the DCR is an Individual/Organization email or address change.
 */
public class ResolveContactChangeContext implements WorkflowAction {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @WorkflowService
    ReltioApi reltioApi;

    @WorkflowService
    ExecutionService executionService;

    @Override
    public void execute(Execution execution) throws Exception {
        String base = Utility.generateTenantUrl(execution.getEnvironmentUrl(), execution.getTenantId()) + "/";
        String token = execution.getAccessToken();

        String dcrUri = ContactChangeAuditSupport.findChangeRequestUri(execution.getObjectUris());
        ContactChangeAuditSupport.ResolvedContext resolved =
                new ContactChangeAuditSupport.ResolvedContext("UNKNOWN", false, false);

        if (dcrUri != null) {
            Map<String, Object> dcr = MAPPER.readValue(
                    reltioApi.invokeApi(token, base + dcrUri + "?showObjectsInfo=true", "GET", ""), Map.class);
            boolean contactChange = ContactChangeAuditSupport.resolveFromDcrMap(dcr).contactChange();
            String entityType = resolveEntityTypeFromDcr(base, token, dcr);
            boolean tripleReview = contactChange && isPartyType(entityType) && !"MIXED".equals(entityType);
            resolved = new ContactChangeAuditSupport.ResolvedContext(entityType, tripleReview, contactChange);
        }

        executionService.setVariable(
                execution.getId(), ContactChangeAuditSupport.ENTITY_TYPE_VARIABLE, resolved.entityType());
        executionService.setVariable(
                execution.getId(),
                ContactChangeAuditSupport.CONTACT_TRIPLE_REVIEW_VARIABLE,
                resolved.contactChangeTripleReview());

        String primaryEntity = ContactChangeAuditSupport.findPrimaryEntityUri(execution.getObjectUris());
        if (primaryEntity != null) {
            executionService.setVariable(execution.getId(), "primaryEntityUri", primaryEntity);
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveEntityTypeFromDcr(String base, String token, Map<String, Object> dcr) throws Exception {
        Map<String, List<Map<String, Object>>> changes =
                (Map<String, List<Map<String, Object>>>) dcr.get("changes");
        if (changes == null) {
            return "UNKNOWN";
        }

        Set<String> types = new HashSet<>();
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

        if (types.size() == 1) {
            return types.iterator().next();
        }
        if (types.isEmpty()) {
            return "UNKNOWN";
        }
        return "MIXED";
    }

    private static boolean isPartyType(String entityType) {
        return ContactChangeAuditSupport.INDIVIDUAL_TYPE.equals(entityType)
                || ContactChangeAuditSupport.ORGANIZATION_TYPE.equals(entityType);
    }
}