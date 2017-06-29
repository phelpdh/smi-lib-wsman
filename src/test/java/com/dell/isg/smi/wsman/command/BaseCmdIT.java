/**************************************************************************
 *   Copyright (c) 2013 Dell Inc. All rights reserved.                    *
 *                                                                        *
 * DELL INC. CONFIDENTIAL AND PROPRIETARY INFORMATION. This software may  *
 * only be supplied under the terms of a license agreement or             *
 * nondisclosure agreement with Dell Inc. and may not be copied or        *
 * disclosed except in accordance with the terms of such agreement.       *
 **************************************************************************/
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
