package com.reltio.workflow.external.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.Utility;
import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.WorkflowAction;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.api.rest.ReltioConstants;
import com.reltio.workflow.api.rest.ReltioException;
import com.reltio.workflow.api.rest.beans.ReltioResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Perform data change request action on the given objectURI by 3rdParty
 */
abstract class ChangeRequest3rdPartyAction implements WorkflowAction {

    private static final String RELTIO_CALL_DCR_ACTION = "%s/%s/%s?ignoreConflicts=true";
    private static final Logger logger = LoggerFactory.getLogger(ChangeRequest3rdPartyAction.class);

    @WorkflowService
    protected ReltioApi reltioApi;

    abstract String getAction();

    public void execute(Execution execution) throws Exception {
        logger.info("In ChangeRequest3rdPartyAction class (action = {})", getAction());
        final String accessToken = execution.getAccessToken();
        final String environmentURL = execution.getEnvironmentUrl();
        final String tenantId = execution.getTenantId();

        List<String> objectUris = execution.getObjectUris();
        String changeRequestUri = Utils.getChangeRequestUri(objectUris);

        if (changeRequestUri == null) {
            throw new ReltioException("No change request found for objectUris = " + objectUris);
        }

        // Reltio Call for "action"
        String decisionUrl = String.format(RELTIO_CALL_DCR_ACTION, Utility.generateTenantUrl(environmentURL, tenantId),
                changeRequestUri, getAction());

        String responseJson = reltioApi.invokeApi(accessToken, decisionUrl, "POST", "");
        ObjectMapper objectMapper = new ObjectMapper();
        ReltioResponse reltioResponse = objectMapper.readValue(responseJson, ReltioResponse.class);
        if (reltioResponse.getStatus() == null || !reltioResponse.getStatus().equals(ReltioConstants.SUCCESS)) {
            logger.error("{}: {}", reltioResponse.getErrorCode(), reltioResponse.getErrorMessage());
            throw new ReltioException(responseJson);
        }
        logger.info(responseJson);
    }

}
