package com.company.workflow.location;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.rest.ReltioApi;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ResolveRegionalReviewerGroupTest {

    @Test
    void setsUsGroupFromEntityCountry() throws Exception {
        ResolveRegionalReviewerGroup action = new ResolveRegionalReviewerGroup();
        action.reltioApi = Mockito.mock(ReltioApi.class);
        action.executionService = Mockito.mock(ExecutionService.class);

        Execution execution = mockExecution();
        String dcrJson = new ObjectMapper().writeValueAsString(Map.of(
                "changes", Map.of(
                        "entities/loc1", List.of(Map.of("type", "UPDATE_ATTRIBUTE")))));
        String entityJson = new ObjectMapper().writeValueAsString(Map.of(
                "type", "configuration/entityTypes/Location",
                "attributes", Map.of(
                        "Country", List.of(Map.of("value", "United States")))));

        Mockito.when(action.reltioApi.invokeApi("token", "environmentURL/reltio/api/tenantId/changeRequests/cr1", "GET", ""))
                .thenReturn(dcrJson);
        Mockito.when(action.reltioApi.invokeApi("token", "environmentURL/reltio/api/tenantId/entities/loc1", "GET", ""))
                .thenReturn(entityJson);

        action.execute(execution);

        Mockito.verify(action.executionService).setVariable("executionId",
                RegionalReviewerRouting.VARIABLE_REGIONAL_GROUP, RegionalReviewerRouting.GROUP_US);
    }

    @Test
    void prefersCountryOnCreateEntity() throws Exception {
        ResolveRegionalReviewerGroup action = new ResolveRegionalReviewerGroup();
        action.reltioApi = Mockito.mock(ReltioApi.class);
        action.executionService = Mockito.mock(ExecutionService.class);

        Execution execution = mockExecution();
        String dcrJson = new ObjectMapper().writeValueAsString(Map.of(
                "changes", Map.of(
                        "entities/newLoc", List.of(Map.of(
                                "type", "CREATE_ENTITY",
                                "newValue", Map.of(
                                        "type", "configuration/entityTypes/Location",
                                        "attributes", Map.of(
                                                "Country", List.of(Map.of("value", "France")))))))));

        Mockito.when(action.reltioApi.invokeApi("token", "environmentURL/reltio/api/tenantId/changeRequests/cr1", "GET", ""))
                .thenReturn(dcrJson);

        action.execute(execution);

        Mockito.verify(action.executionService).setVariable("executionId",
                RegionalReviewerRouting.VARIABLE_REGIONAL_GROUP, RegionalReviewerRouting.GROUP_EU);
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
