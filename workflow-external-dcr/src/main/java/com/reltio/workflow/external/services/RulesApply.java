package com.reltio.workflow.external.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.actions.WorkflowAction;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.external.services.beans.ChangeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RulesApply implements WorkflowAction {

	public static final Logger logger = LoggerFactory.getLogger(RulesApply.class);
	private static final String EXTERNAL_REVIEW = "externalReview";

	@WorkflowService
	ExecutionService executionService;

	@WorkflowService
	ReltioApi reltioApi;

	@Override
	public void execute(Execution delegateExecution) throws Exception {
		logger.info("In RulesApply class");
		List<String> objectUris = delegateExecution.getObjectUris();
		logger.info("ObjectUris = {}", objectUris);
		String tenantId = delegateExecution.getTenantId();

		String changeRequestUri = Utils.getChangeRequestUri(objectUris);
		String externalReviewRequired = "internal";

		if (changeRequestUri != null) {
			logger.info("ChangeRequestUri = {}", changeRequestUri);
            String url = delegateExecution.getEnvironmentUrl() + "/reltio/api/" + tenantId + "/" + changeRequestUri
					+ "?showObjectsInfo=true";
			String responseJson = reltioApi.invokeApi(delegateExecution.getAccessToken(), url, "GET", "");
			ObjectMapper objectMapper = new ObjectMapper();
			ChangeRequest changeRequest = objectMapper.readValue(responseJson, ChangeRequest.class);
			for (String entityUri : Utils.getEntityUris(objectUris))
			{
				if (Utils.isCreateEntity(changeRequest, entityUri))
				{
					externalReviewRequired = "external";
				}
			}
		}

		executionService.setVariable(delegateExecution.getId(), EXTERNAL_REVIEW, externalReviewRequired);
	}

}
