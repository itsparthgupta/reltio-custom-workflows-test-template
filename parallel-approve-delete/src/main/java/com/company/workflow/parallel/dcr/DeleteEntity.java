package com.company.workflow.parallel.dcr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.activiti.service.Delete;
import com.reltio.workflow.api.Utility;
import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.api.rest.ReltioConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DeleteEntity extends Delete
{
	@WorkflowService
	ReltioApi reltioApi;

	@Override
	public void execute(Execution execution) throws Exception
	{
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Integer> value = new HashMap<>();
		// some random values
		value.put("value", 42);
		value.put("key", 24);
		String uri = execution.getObjectUris().get(0);
		String url = Utility.generateTenantUrl(execution.getEnvironmentUrl(), execution.getTenantId()) + "/" + uri
				+ "/attributes/Long";
		reltioApi.invokeApi(execution.getAccessToken(), url, ReltioConstants.POST,
				objectMapper.writeValueAsString(Collections.singletonList(value)));
	}
}
