/**
 * Copyright © 2017 DELL Inc. or its subsidiaries.  All Rights Reserved.
 */
package com.dell.isg.smi.wsman.command;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.dell.isg.smi.wsman.IdracWSManClient;
import com.dell.isg.smi.wsman.WSManClientFactory;
import com.dell.isg.smi.wsman.WSManBaseCommand.WSManClassEnum;
import com.dell.isg.smi.wsmanclient.WSManException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EnumerateSystemViewIT
{
	 protected IdracWSManClient client;

	 @Before
	 public void setUp()
	 {
	  client = WSManClientFactory.getIdracWSManClient("foo", "bar", "foo");
	 }

	 @After
	 public void tearDown()
	 {
	  client.close();
	 }
	
	
	@Test
	public void shouldFindKnownStringValueInJSonObject() throws IOException, WSManException, Exception
	{
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("responses/DCIM_SystemView/1.xml");
	 
		String xml = IOUtils.toString(inputStream, "UTF-8");
		WsmanEnumerate wsEnumerate = new WsmanEnumerate(WSManClassEnum.DCIM_SystemView.name());
		Object systemViewObject = wsEnumerate.parse(xml);
		//Object systemViewObject = client.execute(wsEnumerate);
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(systemViewObject);
		 
		JSONObject jsonObject = new JSONObject(jsonInString);
		String serviceTag = jsonObject.getString("ServiceTag");
		 
		assertEquals("Service tag does not match expected value", "4V63TS1", serviceTag);
	}
}
