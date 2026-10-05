package com.company.workflow.product;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.api.rest.ReltioException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CaptureRequestorTest {

    @Test
    void successTest() throws Exception {
        CaptureRequestor captureRequestor = new CaptureRequestor();
        captureRequestor.reltioApi = Mockito.mock(ReltioApi.class);
        captureRequestor.executionService = Mockito.mock(ExecutionService.class);

        Execution execution = Mockito.mock(Execution.class);
        Mockito.when(execution.getAccessToken()).thenReturn("accessToken");
        Mockito.when(execution.getEnvironmentUrl()).thenReturn("environmentURL");
        Mockito.when(execution.getTenantId()).thenReturn("tenantId");
        Mockito.when(execution.getId()).thenReturn("executionId");
        Mockito.when(execution.getObjectUris()).thenReturn(List.of("entities/objectUri1", "changeRequests/objectUri2"));

        String url = "environmentURL/reltio/api/tenantId/changeRequests/objectUri2";
        Mockito.when(captureRequestor.reltioApi.invokeApi("accessToken", url, "GET", ""))
                .thenReturn(new ObjectMapper().writeValueAsString(Map.of("createdBy", "jane.doe@company.com")));

        captureRequestor.execute(execution);

        Mockito.verify(captureRequestor.executionService, Mockito.times(1))
                .setVariable("executionId", "requestor", "jane.doe@company.com");
    }

    @Test
    void missedDcrUriTest() {
        CaptureRequestor captureRequestor = new CaptureRequestor();
        captureRequestor.reltioApi = Mockito.mock(ReltioApi.class);
        captureRequestor.executionService = Mockito.mock(ExecutionService.class);

        Execution execution = Mockito.mock(Execution.class);
        Mockito.when(execution.getObjectUris()).thenReturn(List.of("entities/objectUri1"));

        ReltioException reltioException = Assertions.assertThrows(ReltioException.class,
                () -> captureRequestor.execute(execution));
        Assertions.assertTrue(reltioException.getMessage().contains("No change request found"));
        Mockito.verifyNoInteractions(captureRequestor.reltioApi, captureRequestor.executionService);
    }
}
