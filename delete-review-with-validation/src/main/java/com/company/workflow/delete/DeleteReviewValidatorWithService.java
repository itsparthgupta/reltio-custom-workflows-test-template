package com.company.workflow.delete;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;
import com.reltio.workflow.api.validators.TaskValidator;
import com.reltio.workflow.api.validators.ValidationError;

public class DeleteReviewValidatorWithService implements TaskValidator
{
	@WorkflowService
	TaskService taskService;

	@Override
	public void doValidate(Task task) throws Exception
	{
		String validatorName = taskService.getStringFormProperty(task, "validator");
		if (validatorName != null)
		{
			throw new ValidationError("I am test validation error");
		}
	}
}
