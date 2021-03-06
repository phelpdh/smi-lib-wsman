/**
 * Copyright � 2017 DELL Inc. or its subsidiaries.  All Rights Reserved.
 */
package com.dell.isg.smi.wsman.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.dell.isg.smi.wsmanclient.WSManBaseInvokeCommand;
import com.dell.isg.smi.wsmanclient.WSManException;
import com.dell.isg.smi.wsmanclient.model.InvokeCmdResponse;
import com.dell.isg.smi.wsmanclient.util.UpdateUtils;

/**
 * The Class InvokeClearServerSelLogCmd.
 */
public class InvokeClearServerSelLogCmd extends WSManBaseInvokeCommand<InvokeCmdResponse> {

    /**
     * Instantiates a new invoke clear server sel log cmd.
     */
    public InvokeClearServerSelLogCmd() {
        super(WSManInvokableEnum.DCIM_SelRecordLog, WSManMethodEnum.CLEAR_SEL_LOG.toString());
    }

    /* (non-Javadoc)
     * @see com.dell.isg.smi.wsmanclient.IWSManCommand#getFilterWQL()
     */
    @Override
    public String getFilterWQL() {

        return null;
    }

    /* (non-Javadoc)
     * @see com.dell.isg.smi.wsmanclient.IWSManCommand#parse(java.lang.String)
     */
    @Override
    public InvokeCmdResponse parse(String xml) throws WSManException {
        String uri = getResourceURI();
        return UpdateUtils.getAsInvokeCmdResponse(xml, uri);
    }
    
    /* (non-Javadoc)
     * @see com.dell.isg.smi.wsmanclient.WSManBaseInvokeCommand#getSelectors()
     */
    @Override
    public List<Pair<String, String>> getSelectors() {
        List<Pair<String, String>> ret = new ArrayList<Pair<String, String>>();
        ret.add(Pair.of("InstanceID", "DCIM:SEL:1"));
        return ret;
    }

    /* (non-Javadoc)
     * @see com.dell.isg.smi.wsmanclient.WSManBaseInvokeCommand#getUserParams()
     */
    @Override
    public List<Pair<String, String>> getUserParams() {
        List<Pair<String, String>> ret = new ArrayList<Pair<String, String>>();
        return ret;

    }

}
