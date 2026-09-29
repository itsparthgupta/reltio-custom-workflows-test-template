package com.reltio.workflow.custom.notifications;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.actions.WorkflowAction;
import com.reltio.workflow.api.notification.mail.Notification;
import com.reltio.workflow.api.notification.mail.NotificationSenderService;

public class OverdueNotification implements WorkflowAction {
    @WorkflowService
    NotificationSenderService notificationSenderService;
    @WorkflowService
    ExecutionService executionService;

    @Override
    public void execute(Execution execution) {
        Notification notification = getNotification(execution);
        this.notificationSenderService.sendNotification(notification);
    }

    private Notification getNotification(Execution execution) {
        String inboxLink = "https://test-usg-workflowui.reltio.com/" + execution.getTenantId() + "/inbox";
        Notification notification = new Notification(execution.getTenantId());
        notification.setSubject("Your task is overdue");
        notification.setBody(
                "<table style=\"width:500px\">" +
                    "<tbody>" +
                        "<tr>" +
                            "<td>" +
                                "<p>REMINDER</p>" +
                                "<p>The task assigned to you is overdue. Please make a decision - <a href=\"" + inboxLink + "\">Inbox</a></p>" +
                            "</td>" +
                        "</tr>" +
                    "</tbody>" +
                "</table>");


        //Email address taken from variables
        String dcrAssignee = executionService.getVariable(execution.getId(), "dcrAssignee", String.class);
        if (dcrAssignee != null && !dcrAssignee.isEmpty()) {
            notification.addTo(dcrAssignee);
        }
        return notification;
    }
}
