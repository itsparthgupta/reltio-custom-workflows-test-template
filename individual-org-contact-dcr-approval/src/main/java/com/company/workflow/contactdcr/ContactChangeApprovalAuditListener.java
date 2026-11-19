package com.company.workflow.contactdcr;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.listeners.WorkflowTaskListener;
import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;

/**
 * Queues one audit row on the process for flush to {@code DcrApprovalAuditTrail} at apply/reject.
 */
public class ContactChangeApprovalAuditListener implements WorkflowTaskListener {

    static final String PENDING_AUDIT_VARIABLE = "contactApprovalAuditPending";

    @WorkflowService
    TaskService taskService;

    @WorkflowService
    ExecutionService executionService;

    @Override
    public void notify(Task task) {
        Boolean tripleReview = executionService.getVariable(
                task.getExecutionId(), ContactChangeAuditSupport.CONTACT_TRIPLE_REVIEW_VARIABLE, Boolean.class);
        if (tripleReview == null || !tripleReview) {
            return;
        }

        String decision = stringVar(task, "decision");
        String comment = stringVar(task, "comment");
        String reviewer = task.getAssignee() != null ? task.getAssignee() : "";
        String step = task.getName() != null ? task.getName() : task.getTaskDefinitionKey();

        Map<String, String> row = new HashMap<>();
        row.put("step", step);
        row.put("decision", decision);
        row.put("reviewer", reviewer);
        row.put("comment", comment);
        row.put("recordedAt", Instant.now().toString());

        List<Map<String, String>> pending = executionService.getVariable(
                task.getExecutionId(), PENDING_AUDIT_VARIABLE, List.class);
        if (pending == null) {
            pending = new ArrayList<>();
        } else {
            pending = new ArrayList<>(pending);
        }
        pending.add(row);
        executionService.setVariable(task.getExecutionId(), PENDING_AUDIT_VARIABLE, pending);
    }

    private String stringVar(Task task, String name) {
        Object value = taskService.getVariableLocal(task.getId(), name);
        return value == null ? "" : String.valueOf(value);
    }
}