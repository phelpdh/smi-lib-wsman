/**
 * Copyright © 2017 DELL Inc. or its subsidiaries.  All Rights Reserved.
 */
package com.dell.isg.smi.wsman.command;

import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNotNull;

import java.io.IOException;

import org.json.JSONObject;
import org.junit.Test;

import com.dell.isg.smi.wsman.WSManBaseCommand.WSManClassEnum;
import com.dell.isg.smi.wsmanclient.WSManException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EnumerateSystemViewParseTest extends BaseCmdIT
{
	@Test
	public void shouldFindKnownStringValueInJSonObject() throws IOException, WSManException, Exception
	{
		WsmanEnumerate wsEnumerate = new WsmanEnumerate(WSManClassEnum.DCIM_SystemView.name());
		Object systemViewObject = client.execute(wsEnumerate);
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(systemViewObject);
		 
		JSONObject jsonObject = new JSONObject(jsonInString);
		String serviceTag = jsonObject.getString("ServiceTag");
		
		assertNotNull("Service tag is null", serviceTag );
		assertNotSame("Service tag is empty", "", serviceTag);
	}
}
