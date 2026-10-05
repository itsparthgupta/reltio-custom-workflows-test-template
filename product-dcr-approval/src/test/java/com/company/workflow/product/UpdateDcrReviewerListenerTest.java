package com.company.workflow.product;

import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UpdateDcrReviewerListenerTest {

    @Test
    void storesAssigneeOnComplete() {
        UpdateDcrReviewerListener listener = new UpdateDcrReviewerListener();
        listener.taskService = Mockito.mock(TaskService.class);

        Task task = Mockito.mock(Task.class);
        Mockito.when(task.getId()).thenReturn("task-1");
        Mockito.when(task.getAssignee()).thenReturn("reviewer@company.com");

        listener.notify(task);

        Mockito.verify(listener.taskService).setVariable("task-1", "dcrReviewer", "reviewer@company.com");
    }
}
