package com.tue.scservice;

public class SemConBroadcastReceiver
extends ALongRunningReceiver
{
	@Override
	public Class getLRSClass() 
	{
		//Utils.logThreadSignature("Test30SecBCR");
		return SemConBCRService.class;
	}
}
