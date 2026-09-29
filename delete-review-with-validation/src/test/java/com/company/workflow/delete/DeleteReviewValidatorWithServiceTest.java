package com.company.workflow.delete;

import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;
import com.reltio.workflow.api.validators.ValidationError;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class DeleteReviewValidatorWithServiceTest {
    @Test
    void testValidateSuccess() {
        DeleteReviewValidatorWithService deleteReviewValidator = new DeleteReviewValidatorWithService();
        deleteReviewValidator.taskService = Mockito.mock(TaskService.class);
        Task task = Mockito.mock(Task.class);
        assertDoesNotThrow(() -> deleteReviewValidator.doValidate(task));
    }

    @Test
    void testValidateFail() {
        DeleteReviewValidatorWithService deleteReviewValidator = new DeleteReviewValidatorWithService();
        Task task = Mockito.mock(Task.class);
        deleteReviewValidator.taskService = Mockito.mock(TaskService.class);
        Mockito.when(deleteReviewValidator.taskService.getStringFormProperty(task, "validator")).thenReturn("test");
        ValidationError validationError = assertThrows(ValidationError.class, () -> deleteReviewValidator.doValidate(task));
        assertEquals("I am test validation error", validationError.getMessage());
    }
}