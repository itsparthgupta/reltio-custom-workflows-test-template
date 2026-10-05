package com.company.workflow.product;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.listeners.WorkflowTaskListener;
import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;

/**
 * Persists the reviewer who completed the task so the next review cycle returns to them.
 */
public class UpdateDcrReviewerListener implements WorkflowTaskListener {

    static final String DCR_REVIEWER_VARIABLE = "dcrReviewer";

    @WorkflowService
    TaskService taskService;

    @Override
    public void notify(Task task) {
        taskService.setVariable(task.getId(), DCR_REVIEWER_VARIABLE, task.getAssignee());
    }
}
