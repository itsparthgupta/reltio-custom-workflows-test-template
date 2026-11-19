package com.company.workflow.contactdcr;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.rest.ReltioApi;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ResolveContactChangeContextTest {

    @Test
    void setsTripleReviewForOrganizationEmailChange() throws Exception {
        ResolveContactChangeContext action = new ResolveContactChangeContext();
        action.reltioApi = Mockito.mock(ReltioApi.class);
        action.executionService = Mockito.mock(ExecutionService.class);

        Execution execution = Mockito.mock(Execution.class);
        Mockito.when(execution.getAccessToken()).thenReturn("token");
        Mockito.when(execution.getEnvironmentUrl()).thenReturn("environmentURL");
        Mockito.when(execution.getTenantId()).thenReturn("tenantId");
        Mockito.when(execution.getId()).thenReturn("executionId");
        Mockito.when(execution.getObjectUris()).thenReturn(List.of("entities/e1", "changeRequests/cr1"));

        String dcrJson = new ObjectMapper().writeValueAsString(Map.of(
                "changes", Map.of(
                        "entities/e1", List.of(Map.of(
                                "type", "UPDATE_ATTRIBUTE",
                                "attributeType",
                                "configuration/entityTypes/Organization/attributes/Email/attributes/Email")))));
        String entityJson = new ObjectMapper().writeValueAsString(
                Map.of("type", ContactChangeAuditSupport.ORGANIZATION_TYPE));

        Mockito.when(action.reltioApi.invokeApi(
                        "token", "environmentURL/reltio/api/tenantId/changeRequests/cr1?showObjectsInfo=true", "GET", ""))
                .thenReturn(dcrJson);
        Mockito.when(action.reltioApi.invokeApi("token", "environmentURL/reltio/api/tenantId/entities/e1", "GET", ""))
                .thenReturn(entityJson);

        action.execute(execution);

        Mockito.verify(action.executionService).setVariable(
                "executionId", ContactChangeAuditSupport.CONTACT_TRIPLE_REVIEW_VARIABLE, true);
        Mockito.verify(action.executionService).setVariable(
                "executionId", ContactChangeAuditSupport.ENTITY_TYPE_VARIABLE,
                ContactChangeAuditSupport.ORGANIZATION_TYPE);
    }
}