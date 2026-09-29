package com.reltio.workflow.external.services.beans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeRequest
{

	@JsonProperty("uri")
	private String uri;
	@JsonProperty("createdBy")
	private String createdBy;
	@JsonProperty("createdTime")
	private Long createdTime;
	@JsonProperty("updatedBy")
	private String updatedBy;
	@JsonProperty("updatedTime")
	private Long updatedTime;
	@JsonProperty("changes")
	private Map<String, List<Object>> changes;
	@JsonProperty("state")
	private String state;
	private Map<String, Map<String, Object>> objectsInfo = new HashMap<>();
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<>();

	/**
	 * @return The uri
	 */
	@JsonProperty("uri")
	public String getUri()
	{
		return uri;
	}

	/**
	 * @param uri
	 *            The uri
	 */
	@JsonProperty("uri")
	public void setUri(String uri)
	{
		this.uri = uri;
	}

	/**
	 * @return The createdBy
	 */
	@JsonProperty("createdBy")
	public String getCreatedBy()
	{
		return createdBy;
	}

	/**
	 * @param createdBy
	 *            The createdBy
	 */
	@JsonProperty("createdBy")
	public void setCreatedBy(String createdBy)
	{
		this.createdBy = createdBy;
	}

	/**
	 * @return The createdTime
	 */
	@JsonProperty("createdTime")
	public Long getCreatedTime()
	{
		return createdTime;
	}

	/**
	 * @param createdTime
	 *            The createdTime
	 */
	@JsonProperty("createdTime")
	public void setCreatedTime(Long createdTime)
	{
		this.createdTime = createdTime;
	}

	/**
	 * @return The updatedBy
	 */
	@JsonProperty("updatedBy")
	public String getUpdatedBy()
	{
		return updatedBy;
	}

	/**
	 * @param updatedBy
	 *            The updatedBy
	 */
	@JsonProperty("updatedBy")
	public void setUpdatedBy(String updatedBy)
	{
		this.updatedBy = updatedBy;
	}

	/**
	 * @return The updatedTime
	 */
	@JsonProperty("updatedTime")
	public Long getUpdatedTime()
	{
		return updatedTime;
	}

	/**
	 * @param updatedTime
	 *            The updatedTime
	 */
	@JsonProperty("updatedTime")
	public void setUpdatedTime(Long updatedTime)
	{
		this.updatedTime = updatedTime;
	}

	/**
	 * @return The changes
	 */
	@JsonProperty("changes")
	public Map<String, List<Object>> getChanges()
	{
		return changes;
	}

	/**
	 * @param changes
	 *            The changes
	 */
	@JsonProperty("changes")
	public void setChanges(Map<String, List<Object>> changes)
	{
		this.changes = changes;
	}

	/**
	 * @return The state
	 */
	@JsonProperty("state")
	public String getState()
	{
		return state;
	}

	/**
	 * @param state
	 *            The state
	 */
	@JsonProperty("state")
	public void setState(String state)
	{
		this.state = state;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties()
	{
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value)
	{
		this.additionalProperties.put(name, value);
	}

	public Map<String, Map<String, Object>> getObjectsInfo()
	{
		return objectsInfo;
	}

	public void setObjectsInfo(Map<String, Map<String, Object>> objectsInfo)
	{
		this.objectsInfo = objectsInfo;
	}
}