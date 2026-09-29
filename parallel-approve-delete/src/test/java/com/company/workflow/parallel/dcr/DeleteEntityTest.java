package com.company.workflow.parallel.dcr;

import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.api.rest.ReltioConstants;
import com.reltio.workflow.api.rest.ReltioException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DeleteEntityTest {

    @Test
    void parallelApproveTest() throws ReltioException {
        DeleteEntity deleteEntity = new DeleteEntity();
        Execution mockExecution = Mockito.mock(Execution.class);
        Mockito.when(mockExecution.getObjectUris()).thenReturn(Collections.singletonList("testUri"));
        Mockito.when(mockExecution.getEnvironmentUrl()).thenReturn("testEnvironmentUrl");
        Mockito.when(mockExecution.getTenantId()).thenReturn("testTenantId");
        Mockito.when(mockExecution.getAccessToken()).thenReturn("testAccessToken");
        ReltioApi mockReltioApi = Mockito.mock(ReltioApi.class);
        deleteEntity.reltioApi = mockReltioApi;
        assertDoesNotThrow(() -> deleteEntity.execute(mockExecution));
        Mockito.verify(mockReltioApi, Mockito.times(1))
                .invokeApi("testAccessToken",
                        "testEnvironmentUrl/reltio/api/testTenantId/testUri/attributes/Long",
                        ReltioConstants.POST,
                        "[{\"value\":42,\"key\":24}]");

    }
}