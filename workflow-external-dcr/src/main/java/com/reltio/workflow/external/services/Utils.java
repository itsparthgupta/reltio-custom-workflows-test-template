package com.reltio.workflow.external.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reltio.workflow.external.services.beans.ChangeRequest;

public class Utils
{
	private static final Pattern EXTRACT_CHANGE_REQUEST_PATTERN = Pattern.compile("^(changeRequests/[A-Za-z0-9]*).*");
	private static final Pattern EXTRACT_ENTITY_PATTERN = Pattern.compile("(entities/[A-Za-z0-9]*).*");

	public static String getChangeRequestUri(List<String> objectUris)
	{
		String changeRequestUri = null;
		for (String objectUri : objectUris)
		{
			Matcher matcher = EXTRACT_CHANGE_REQUEST_PATTERN.matcher(objectUri);
			if (matcher.find())
			{
				changeRequestUri = matcher.group(1);
			}
		}
		return changeRequestUri;
	}

	public static boolean isCreateEntity(ChangeRequest changeRequest, String entityUri)
	{
		if (changeRequest.getChanges() != null)
		{
			List<Object> entityChanges = changeRequest.getChanges().get(entityUri);
			if (entityChanges != null)
			{
				for (Object change : entityChanges)
				{
					Map<String, Object> changeMap = (Map<String, Object>) change;
					if ("CREATE_ENTITY".equals(changeMap.get("type")))
					{
						return true;
					}
				}
			}
		}
		return false;

	}

	public static List<String> getEntityUris(List<String> objectUris)
	{
		List<String> entities = new ArrayList<>();
		for (String objectUri : objectUris)
		{
			Matcher matcher = EXTRACT_ENTITY_PATTERN.matcher(objectUri);
			if (matcher.find())
			{
				entities.add(matcher.group(1));
			}
		}
		return entities;
	}
}
