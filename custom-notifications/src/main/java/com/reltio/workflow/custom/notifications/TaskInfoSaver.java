package com.reltio.workflow.custom.notifications;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.listeners.WorkflowTaskListener;
import com.reltio.workflow.api.notification.mail.NotificationSenderService;
import com.reltio.workflow.api.tasks.Task;

public class TaskInfoSaver implements WorkflowTaskListener {
    @WorkflowService
    NotificationSenderService notificationSenderService;
    @WorkflowService
    ExecutionService executionService;

    @Override
    public void notify(Task task) {
        String assigneeAddress = notificationSenderService.resolveTaskAssigneeEmail(task);
        if (assigneeAddress != null && !assigneeAddress.isEmpty()) {
            executionService.setVariable(task.getExecutionId(), "dcrAssignee", assigneeAddress);
        }
    }
}
