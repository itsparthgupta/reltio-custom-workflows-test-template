package com.company.workflow.product;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.rest.ReltioApi;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ResolveEntityTypeTest {

    @Test
    void createEntitySetsProductType() throws Exception {
        ResolveEntityType action = new ResolveEntityType();
        action.reltioApi = Mockito.mock(ReltioApi.class);
        action.executionService = Mockito.mock(ExecutionService.class);

        Execution execution = mockExecution();
        String dcrJson = new ObjectMapper().writeValueAsString(Map.of(
                "changes", Map.of(
                        "entities/abc", List.of(Map.of(
                                "type", "CREATE_ENTITY",
                                "newValue", Map.of("type", "configuration/entityTypes/Product"))))));
        Mockito.when(action.reltioApi.invokeApi("token", "environmentURL/reltio/api/tenantId/changeRequests/cr1", "GET", ""))
                .thenReturn(dcrJson);

        action.execute(execution);

        Mockito.verify(action.executionService).setVariable("executionId", "entityType",
                "configuration/entityTypes/Product");
    }

    @Test
    void mixedTypesSetMixed() throws Exception {
        ResolveEntityType action = new ResolveEntityType();
        action.reltioApi = Mockito.mock(ReltioApi.class);
        action.executionService = Mockito.mock(ExecutionService.class);

        Execution execution = mockExecution();
        String dcrJson = new ObjectMapper().writeValueAsString(Map.of(
                "changes", Map.of(
                        "entities/a", List.of(Map.of(
                                "type", "CREATE_ENTITY",
                                "newValue", Map.of("type", "configuration/entityTypes/Product"))),
                        "entities/b", List.of(Map.of(
                                "type", "CREATE_ENTITY",
                                "newValue", Map.of("type", "configuration/entityTypes/Location"))))));
        Mockito.when(action.reltioApi.invokeApi("token", "environmentURL/reltio/api/tenantId/changeRequests/cr1", "GET", ""))
                .thenReturn(dcrJson);

        action.execute(execution);

        Mockito.verify(action.executionService).setVariable("executionId", "entityType", "MIXED");
    }

    @Test
    void existingEntityFetchesType() throws Exception {
        ResolveEntityType action = new ResolveEntityType();
        action.reltioApi = Mockito.mock(ReltioApi.class);
        action.executionService = Mockito.mock(ExecutionService.class);

        Execution execution = mockExecution();
        String dcrJson = new ObjectMapper().writeValueAsString(Map.of(
                "changes", Map.of(
                        "entities/existing", List.of(Map.of("type", "UPDATE_ATTRIBUTE")))));
        String entityJson = new ObjectMapper().writeValueAsString(
                Map.of("type", "configuration/entityTypes/Product"));

        Mockito.when(action.reltioApi.invokeApi("token", "environmentURL/reltio/api/tenantId/changeRequests/cr1", "GET", ""))
                .thenReturn(dcrJson);
        Mockito.when(action.reltioApi.invokeApi("token", "environmentURL/reltio/api/tenantId/entities/existing", "GET", ""))
                .thenReturn(entityJson);

        action.execute(execution);

        Mockito.verify(action.executionService).setVariable("executionId", "entityType",
                "configuration/entityTypes/Product");
    }

    private static Execution mockExecution() {
        Execution execution = Mockito.mock(Execution.class);
        Mockito.when(execution.getAccessToken()).thenReturn("token");
        Mockito.when(execution.getEnvironmentUrl()).thenReturn("environmentURL");
        Mockito.when(execution.getTenantId()).thenReturn("tenantId");
        Mockito.when(execution.getId()).thenReturn("executionId");
        Mockito.when(execution.getObjectUris()).thenReturn(List.of("changeRequests/cr1"));
        return execution;
    }
}
