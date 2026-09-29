package com.reltio.workflow.custom.notifications;

import com.reltio.workflow.api.actions.Execution;
import static org.mockito.ArgumentMatchers.argThat;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

public class OverdueNotificationTest {
    @Test
    public void sendingNotification() {
        OverdueNotification overdueNotification = new OverdueNotification();
        overdueNotification.executionService = Mockito.mock(com.reltio.workflow.api.actions.ExecutionService.class);
        Mockito.when(overdueNotification.executionService.getVariable("testId", "dcrAssignee", String.class)).thenReturn("test@reltio.com");
        overdueNotification.notificationSenderService = Mockito.mock(com.reltio.workflow.api.notification.mail.NotificationSenderService.class);
        Execution execution = Mockito.mock(Execution.class);
        Mockito.when(execution.getId()).thenReturn("testId");
        Mockito.when(execution.getTenantId()).thenReturn("testTenant");
        overdueNotification.execute(execution);
        Mockito.verify(overdueNotification.notificationSenderService).sendNotification(argThat(notification -> {
            assertEquals( "Your task is overdue", notification.getSubject());
            assertEquals(1, notification.getTo().size());
            assertTrue(notification.getTo().contains("test@reltio.com"));
            return true;
        }));
    }
}
