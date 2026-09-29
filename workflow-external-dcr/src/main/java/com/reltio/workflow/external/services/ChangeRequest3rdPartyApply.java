package com.reltio.workflow.external.services;

/**
 * Apply data change request on the given objectURI by 3rdParty
 */
public class ChangeRequest3rdPartyApply extends ChangeRequest3rdPartyAction
{

	@Override
	String getAction()
	{
		return "_apply";
	}

}
