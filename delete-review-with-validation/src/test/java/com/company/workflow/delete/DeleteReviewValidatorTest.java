package com.company.workflow.delete;

import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.validators.ValidationError;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DeleteReviewValidatorTest {
    @Test
    void testValidateSuccess() {
        DeleteReviewValidator deleteReviewValidator = new DeleteReviewValidator();
        Task task = Mockito.mock(Task.class);
        Mockito.when(task.getDueDate()).thenReturn(new Date(System.currentTimeMillis() + 100000));
        assertDoesNotThrow(() -> deleteReviewValidator.doValidate(task));
    }

    @Test
    void testValidateFail() {
        DeleteReviewValidator deleteReviewValidator = new DeleteReviewValidator();
        Task task = Mockito.mock(Task.class);
        Mockito.when(task.getDueDate()).thenReturn(new Date(System.currentTimeMillis() - 100000));
        ValidationError validationError = assertThrows(ValidationError.class, () -> deleteReviewValidator.doValidate(task));
        assertEquals("Incorrect due date of task " + task.getId(), validationError.getMessage());
    }
}