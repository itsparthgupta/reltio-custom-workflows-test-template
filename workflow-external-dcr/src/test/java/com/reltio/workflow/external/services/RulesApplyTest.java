package com.reltio.workflow.external.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.external.services.beans.ChangeRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

class RulesApplyTest {
    @Test
    void successTest() throws Exception {
        final RulesApply clazzInstance = new RulesApply();
        clazzInstance.reltioApi = Mockito.mock(ReltioApi.class);
        Execution execution = Mockito.mock(Execution.class);
        //mock execution
        Mockito.when(execution.getAccessToken()).thenReturn("accessToken");
        Mockito.when(execution.getEnvironmentUrl()).thenReturn("environmentURL");
        Mockito.when(execution.getTenantId()).thenReturn("tenantId");
        Mockito.when(execution.getObjectUris()).thenReturn(List.of("entities/objectUri1", "changeRequests/objectUri2"));
        //mock reltioApi
        ChangeRequest changeRequest = new ChangeRequest();
        //fill changeRequest with random data
        changeRequest.setState("REVIEW_AWAITING");
        changeRequest.setUri("changeRequests/objectUri2");
        changeRequest.setChanges(Map.of("entities/objectUri1", List.of(Map.of("type", "CREATE_ENTITY"))));
        String uri = "environmentURL/reltio/api/tenantId/changeRequests/objectUri2?showObjectsInfo=true";
        Mockito.when(clazzInstance.reltioApi.invokeApi("accessToken", uri, "GET", ""))
                .thenReturn(new ObjectMapper().writeValueAsString(changeRequest));
        //mock executionService
        clazzInstance.executionService = Mockito.mock(ExecutionService.class);
        clazzInstance.execute(execution);
        Mockito.verify(clazzInstance.reltioApi, Mockito.times(1))
                .invokeApi("accessToken", uri, "GET", "");
        Mockito.verify(clazzInstance.executionService, Mockito.times(1))
                .setVariable(execution.getId(), "externalReview", "external");
    }
}