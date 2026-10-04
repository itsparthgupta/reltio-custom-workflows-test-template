package com.company.workflow.location;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.Utility;
import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.actions.WorkflowAction;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.api.rest.ReltioException;

/**
 * Runs once, right after the process starts on a Location DCR.
 *
 * Reads the change request's {@code createdBy} value and stores it as the
 * {@code requestor} process variable, so that later steps (API reject,
 * Admin reject) can assign a follow-up task back to the person who
 * originally submitted the change, regardless of how many approve/reject
 * cycles the DCR goes through.
 */
public class CaptureRequestor implements WorkflowAction {

    private static final Pattern CHANGE_REQUEST_URI_PATTERN =
            Pattern.compile("^(changeRequests/[A-Za-z0-9]*).*");

    static final String REQUESTOR_VARIABLE = "requestor";

    @WorkflowService
    ReltioApi reltioApi;

    @WorkflowService
    ExecutionService executionService;

    @Override
    public void execute(Execution execution) throws Exception {
        String changeRequestUri = findChangeRequestUri(execution.getObjectUris());
        if (changeRequestUri == null) {
            throw new ReltioException("No change request found for objectUris = " + execution.getObjectUris());
        }

        String url = Utility.generateTenantUrl(execution.getEnvironmentUrl(), execution.getTenantId())
                + "/" + changeRequestUri;
        String responseJson = reltioApi.invokeApi(execution.getAccessToken(), url, "GET", "");

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> changeRequest = objectMapper.readValue(responseJson, Map.class);
        Object createdBy = changeRequest.get("createdBy");

        if (createdBy != null && !createdBy.toString().isEmpty()) {
            executionService.setVariable(execution.getId(), REQUESTOR_VARIABLE, createdBy.toString());
        }
    }

    private static String findChangeRequestUri(List<String> objectUris) {
        for (String objectUri : objectUris) {
            Matcher matcher = CHANGE_REQUEST_URI_PATTERN.matcher(objectUri);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
