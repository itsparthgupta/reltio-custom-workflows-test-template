package com.reltio.workflow.external.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.api.rest.ReltioConstants;
import com.reltio.workflow.api.rest.ReltioException;
import com.reltio.workflow.api.rest.beans.ReltioResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.List;

class ChangeRequest3rdPartyActionTest {
    @ParameterizedTest
    @ValueSource(classes = {ChangeRequest3rdPartyApply.class, ChangeRequest3rdPartyReject.class})
    void successTest(Class<ChangeRequest3rdPartyAction> clazz) throws Exception {
        final ChangeRequest3rdPartyAction clazzInstance = clazz.getConstructor().newInstance();
        clazzInstance.reltioApi = Mockito.mock(ReltioApi.class);
        Execution execution = Mockito.mock(Execution.class);
        //mock execution
        Mockito.when(execution.getAccessToken()).thenReturn("accessToken");
        Mockito.when(execution.getEnvironmentUrl()).thenReturn("environmentURL");
        Mockito.when(execution.getTenantId()).thenReturn("tenantId");
        Mockito.when(execution.getObjectUris()).thenReturn(List.of("entities/objectUri1", "changeRequests/objectUri2"));
        //mock reltioApi
        ReltioResponse reltioResponse = new ReltioResponse();
        reltioResponse.setStatus(ReltioConstants.SUCCESS);
        String uri = String.format("environmentURL/reltio/api/tenantId/changeRequests/objectUri2/%s?ignoreConflicts=true",
                clazzInstance.getAction());
        Mockito.when(clazzInstance.reltioApi.invokeApi("accessToken", uri, "POST", ""))
                .thenReturn(new ObjectMapper().writeValueAsString(reltioResponse));
        clazzInstance.execute(execution);
        Mockito.verify(clazzInstance.reltioApi, Mockito.times(1))
                .invokeApi("accessToken", uri, "POST", "");
    }

    @ParameterizedTest
    @ValueSource(classes = {ChangeRequest3rdPartyApply.class, ChangeRequest3rdPartyReject.class})
    void missedDcrUriTest(Class<ChangeRequest3rdPartyAction> clazz) throws Exception {
        final ChangeRequest3rdPartyAction clazzInstance = clazz.getConstructor().newInstance();
        clazzInstance.reltioApi = Mockito.mock(ReltioApi.class);
        Execution execution = Mockito.mock(Execution.class);
        //mock execution
        Mockito.when(execution.getAccessToken()).thenReturn("accessToken");
        Mockito.when(execution.getEnvironmentUrl()).thenReturn("environmentURL");
        Mockito.when(execution.getTenantId()).thenReturn("tenantId");
        Mockito.when(execution.getObjectUris()).thenReturn(List.of("entities/objectUri1", "entities/objectUri2"));
        ReltioException reltioException = Assertions.assertThrows(ReltioException.class,
                () -> clazzInstance.execute(execution));
        Assertions.assertEquals(
                "No change request found for objectUris = [entities/objectUri1, entities/objectUri2]",
                reltioException.getMessage());
        Mockito.verifyNoInteractions(clazzInstance.reltioApi);
    }

    @ParameterizedTest
    @ValueSource(classes = {ChangeRequest3rdPartyApply.class, ChangeRequest3rdPartyReject.class})
    void errorResponseTest(Class<ChangeRequest3rdPartyAction> clazz) throws Exception {
        final ChangeRequest3rdPartyAction clazzInstance = clazz.getConstructor().newInstance();
        clazzInstance.reltioApi = Mockito.mock(ReltioApi.class);
        Execution execution = Mockito.mock(Execution.class);
        //mock execution
        Mockito.when(execution.getAccessToken()).thenReturn("accessToken");
        Mockito.when(execution.getEnvironmentUrl()).thenReturn("environmentURL");
        Mockito.when(execution.getTenantId()).thenReturn("tenantId");
        Mockito.when(execution.getObjectUris()).thenReturn(List.of("entities/objectUri1", "changeRequests/objectUri2"));
        //mock reltioApi
        ReltioResponse reltioResponse = new ReltioResponse();
        reltioResponse.setStatus(ReltioConstants.FAILED);
        reltioResponse.setErrorMessage("error");
        reltioResponse.setErrorCode(1);
        String uri = String.format("environmentURL/reltio/api/tenantId/changeRequests/objectUri2/%s?ignoreConflicts=true",
                clazzInstance.getAction());
        Mockito.when(clazzInstance.reltioApi.invokeApi("accessToken", uri, "POST", ""))
                .thenReturn(new ObjectMapper().writeValueAsString(reltioResponse));
        ReltioException reltioException = Assertions.assertThrows(ReltioException.class,
                () -> clazzInstance.execute(execution));
        Assertions.assertEquals(
                "{\"errorCode\":1,\"errorMessage\":\"error\",\"status\":\"failed\"}",
                reltioException.getMessage());
    }
}