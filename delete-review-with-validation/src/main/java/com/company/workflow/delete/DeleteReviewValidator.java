package com.company.workflow.delete;

import java.util.Date;

import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.validators.TaskValidator;
import com.reltio.workflow.api.validators.ValidationError;

public class DeleteReviewValidator implements TaskValidator
{
	@Override
	public void doValidate(Task task) throws Exception
	{
		if (task.getDueDate().compareTo(new Date()) <= 0)
		{
			throw new ValidationError("Incorrect due date of task " + task.getId());
		}
	}
}
