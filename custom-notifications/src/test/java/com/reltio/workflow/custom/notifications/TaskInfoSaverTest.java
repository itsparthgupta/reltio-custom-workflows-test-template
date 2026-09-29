package com.reltio.workflow.custom.notifications;

import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.notification.mail.NotificationSenderService;
import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TaskInfoSaverTest {
    @Test
    public void emailIsSetToExecutionService() {
        TaskInfoSaver taskInfoSaver = new TaskInfoSaver();
        Task task = Mockito.mock(Task.class);
        Mockito.when(task.getExecutionId()).thenReturn("test");
        taskInfoSaver.executionService = Mockito.mock(ExecutionService.class);
        taskInfoSaver.notificationSenderService = Mockito.mock(NotificationSenderService.class);
        Mockito.when(taskInfoSaver.notificationSenderService.resolveTaskAssigneeEmail(task)).thenReturn("test@reltio.com");
        taskInfoSaver.notify(task);
        Mockito.verify(taskInfoSaver.executionService).setVariable("test", "dcrAssignee", "test@reltio.com");
    }

    @Test
    public void emailIsNotSetToExecutionService() {
        TaskInfoSaver taskInfoSaver = new TaskInfoSaver();
        Task task = Mockito.mock(Task.class);
        Mockito.when(task.getExecutionId()).thenReturn("test");
        taskInfoSaver.executionService = Mockito.mock(ExecutionService.class);
        taskInfoSaver.notificationSenderService = Mockito.mock(NotificationSenderService.class);
        Mockito.when(taskInfoSaver.notificationSenderService.resolveTaskAssigneeEmail(task)).thenReturn("");
        taskInfoSaver.notify(task);
        Mockito.verify(taskInfoSaver.executionService, Mockito.times(0))
                .setVariable("test", "dcrAssignee", "");
    }
}
