/**
 * Copyright © 2017 DELL Inc. or its subsidiaries.  All Rights Reserved.
 */
package com.dell.isg.smi.wsman.command;

import org.junit.After;
import org.junit.Before;

import com.dell.isg.smi.wsman.IdracWSManClient;
import com.dell.isg.smi.wsman.WSManClientFactory;


public abstract class BaseCmdIT
{
	protected IdracWSManClient client;

 @Before
 public void setUp()
 {
  String ip = "100.68.123.160";
  	client = WSManClientFactory.getIdracWSManClient(ip, "root", "calvin");
 }

 @After
 public void tearDown()
 {
  client.close();
 }
}
