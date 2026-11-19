package com.company.workflow.contactdcr;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.Utility;
import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.rest.ReltioApi;

/**
 * Writes queued approval audit rows onto the entity's {@code DcrApprovalAuditTrail} list attribute.
 */
final class ContactChangeAuditFlusher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ContactChangeAuditFlusher() {
    }

    static void flushPending(Execution execution, ReltioApi reltioApi, ExecutionService executionService)
            throws Exception {
        Boolean tripleReview = executionService.getVariable(
                execution.getId(), ContactChangeAuditSupport.CONTACT_TRIPLE_REVIEW_VARIABLE, Boolean.class);
        if (tripleReview == null || !tripleReview) {
            return;
        }

        List<Map<String, String>> pending = executionService.getVariable(
                execution.getId(), ContactChangeApprovalAuditListener.PENDING_AUDIT_VARIABLE, List.class);
        if (pending == null || pending.isEmpty()) {
            return;
        }

        String entityUri = executionService.getVariable(execution.getId(), "primaryEntityUri", String.class);
        if (entityUri == null) {
            entityUri = ContactChangeAuditSupport.findPrimaryEntityUri(execution.getObjectUris());
        }
        if (entityUri == null) {
            return;
        }

        String base = Utility.generateTenantUrl(execution.getEnvironmentUrl(), execution.getTenantId()) + "/";
        String token = execution.getAccessToken();

        String entityJson = reltioApi.invokeApi(token, base + entityUri, "GET", "");
        Map<String, Object> entity = MAPPER.readValue(entityJson, Map.class);
        String body = ContactChangeAuditSupport.buildAuditPatchBodyFromPending(entity, pending);
        reltioApi.invokeApi(token, base + entityUri, "PATCH", body);
    }
}