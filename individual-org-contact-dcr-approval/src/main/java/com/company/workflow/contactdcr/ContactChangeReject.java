package com.company.workflow.contactdcr;

import com.reltio.workflow.activiti.service.ChangeRequestReject;
import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.rest.ReltioApi;

public class ContactChangeReject extends ChangeRequestReject {

    @WorkflowService
    ReltioApi reltioApi;

    @WorkflowService
    ExecutionService executionService;

    @Override
    public void execute(Execution execution) throws Exception {
        ContactChangeAuditFlusher.flushPending(execution, reltioApi, executionService);
        super.execute(execution);
    }
}