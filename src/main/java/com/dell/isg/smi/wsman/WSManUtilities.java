/**
 * Copyright © 2017 DELL Inc. or its subsidiaries.  All Rights Reserved.
 */
package com.dell.isg.smi.wsman;

import static com.dell.isg.smi.commons.utilities.constants.CommonConstants.FIFTEEN_SEC;
import static com.dell.isg.smi.commons.utilities.constants.CommonConstants.FOUR_MIN;
import static com.dell.isg.smi.commons.utilities.constants.CommonConstants.TEN_SEC;
import static com.dell.isg.smi.commons.utilities.constants.CommonConstants.TWENTY_SEC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.dell.isg.smi.commons.utilities.datetime.DateTimeUtils;
import com.dell.isg.smi.wsman.command.DeleteJobCmd;
import com.dell.isg.smi.wsman.command.EnumerateCSIRCmd;
import com.dell.isg.smi.wsman.command.EnumerateJobs;
import com.dell.isg.smi.wsman.command.EnumerateSoftwareIdentityCmd;
import com.dell.isg.smi.wsman.command.GetRSStatusCmd;
import com.dell.isg.smi.wsman.command.GetRSStatusCmd.RSStatusEnum;
import com.dell.isg.smi.wsman.command.GetRemoteServicesAPIStatus;
import com.dell.isg.smi.wsman.command.GetRemoteServicesAPIStatus.RSAPIStatusEnum;
import com.dell.isg.smi.wsman.command.entity.DCIMSoftwareIdentityType;
import com.dell.isg.smi.wsman.command.entity.InstanceCSIR;
import com.dell.isg.smi.wsman.entity.LifeCycleJob;
import com.dell.isg.smi.wsmanclient.util.Trigger;

/**
 *
 * @author Dan_Phelps
 *
 */

public class WSManUtilities {

    private static final Logger logger = LoggerFactory.getLogger(WSManUtilities.class);

    private static final String iDRAC_COMPONENT_ID_12G = "25227";
    private static final Pattern WSMAN_DATE_PATTERN = Pattern.compile("^[0-9]{14}\\.[0-9]{6}\\+[0-9]{3}");
    private static final String WSMAN_DATE_FORMAT = "yyyyMMddHHmmss";

    // List of LC return codes with related messages containing "existing job is completed or is cancelled"
    public static List<String> existingJobCompleteOrCancelErrorCodes = new ArrayList<String>();
    // List of LC return codes with related messages containing "no pending data"
    public static List<String> noPendingDataErrorCodes = new ArrayList<String>();
    // List of LC return codes with related messages containing "no pending data"
    public static List<String> systemServicesInUseErrorCodes = new ArrayList<String>();

    static {
        existingJobCompleteOrCancelErrorCodes.add("BIOS006");
        existingJobCompleteOrCancelErrorCodes.add("BIOS007");
        existingJobCompleteOrCancelErrorCodes.add("NIC006");
        existingJobCompleteOrCancelErrorCodes.add("NIC007");

        noPendingDataErrorCodes.add("BIOS008");
        noPendingDataErrorCodes.add("BIOS012");
        noPendingDataErrorCodes.add("NIC008");
        noPendingDataErrorCodes.add("NIC012");
        noPendingDataErrorCodes.add("PR26");

        systemServicesInUseErrorCodes.add("BIOS009");
        systemServicesInUseErrorCodes.add("NIC009");
    }


    /**
     * This method uses default READY state timeout of 4 minutes. <br>
     * Minimum overall timeout is always 4 minutes. <br>
     * Method should be called to determine state of LC after reboot of server. Upon first constant READY state > 4 minutes or transition from RELOADING to READY state, check CSIOR
     * status of server. If CSIOR is enabled, again check for constant READY state > 4 minutes, or transition from RELOADING to READY state.
     * 
     * @param ip
     * @param user
     * @param password
     * @param timeout
     * <li>Maximum timeout in any state
     * <li>Must be in millis
     * <li>Minimum is 4 minutes
     * @return TRUE if timeout in READY state or transition from RELOADING to READY, FALSE otherwise
     * @throws Exception
     */
    public static boolean waitForLCReadyStatusPostCSIOR(String ip, String user, String password, long timeout) {

        if (waitForLCReadyStatus(ip, user, password, timeout)) { // true if timed out in READY or transitioned to READY
            InstanceCSIR status = null;
            for (int i = 0; i < 5; i++) {
                try {
                    EnumerateCSIRCmd cmd = new EnumerateCSIRCmd(ip, user, password);
                    status = cmd.execute();
                    break;
                } catch (Exception e) {
                    try {
                        Thread.sleep(FIFTEEN_SEC);
                    } catch (InterruptedException ie) {
                    }
                }
            }
            if (status == null)
                return false;
            if (status.currentValue.equalsIgnoreCase("Enabled")) {
                return waitForLCReadyStatus(ip, user, password, timeout); // wait for CSIOR to complete.
            } else {
                return true;
            }
        } else {
            return false;
        }
    }


    /**
     * This method uses default READY state timeout of 4 minutes. Method will block until one of the following occurs: Constant READY state > 4 minutes. Transition from RELOADING
     * to READY state. Current time > timeout while in RELOADING, NOT READY or ERROR state. Minimum timeout is always FOUR_MIN
     * 
     * @param ip
     * @param user
     * @param password
     * @param timeout
     * <li>Maximum timeout in any state
     * <li>Must be in millis
     * <li>Minimum is 4 minutes
     * @return TRUE if READY state, FALSE otherwise
     */
    public static boolean waitForLCReadyStatus(String ip, String user, String password, long timeout) {

        return waitForLCReadyStatus(ip, user, password, timeout, FOUR_MIN, TWENTY_SEC);
    }


    /**
     * This method finds out if the API status is supported. If it is, it will call a method that uses this api. Otherwise it will use the RSStatus way. The API method is preferred
     * because it provides a more accurate stauts without the wait, however it is only suppored in LC firmware 1.5.1 and above.
     * 
     * @param ip
     * @param user
     * @param password
     * @param timeout
     * @param readyStateTimeout
     * @param sleepInterval
     * @return
     */
    public static boolean waitForLCReadyStatus(String ip, String user, String password, long timeout, long readyStateTimeout, long sleepInterval) {
        logger.debug("Entered waitForLCReadyStatus");
        boolean retVal = false;
        boolean bRSAPISupported = false;
        try {
            GetRemoteServicesAPIStatus statusAPICmd = new GetRemoteServicesAPIStatus(ip, user, password);
            RSAPIStatusEnum stausAPI = statusAPICmd.execute();
            if (stausAPI == RSAPIStatusEnum.ERROR) { // TODO check return status for unsupported version
                bRSAPISupported = false;
                logger.debug("GetRemoteServicesAPIStatus is not supported");
            } else {
                bRSAPISupported = true;
                logger.debug("GetRemoteServicesAPIStatus is supported");
            }
        } catch (Exception e) {
            logger.debug("GetRemoteServicesAPIStatus is not supported");
        }

        if (timeout < readyStateTimeout)
            timeout = readyStateTimeout; // set minimum timeout

        if (bRSAPISupported) {
            // use the newer method that supports the GetRemoteServicesAPIStatus call
            retVal = waitForLCReadyWithGetRSAPIStatus(ip, user, password, readyStateTimeout, timeout, sleepInterval);
            logger.debug(String.format("Exiting waitForLCReadyStatus using GetRemoteServicesAPIStatus with value: %s", retVal));
        } else {
            // use the older method that uses the getRSStatus call and has an extra 4 minute wait time
            retVal = waitForLCReadyWithGetRSStatus(ip, user, password, readyStateTimeout, timeout, sleepInterval);
            logger.debug(String.format("Exiting waitForLCReadyStatus using GetRSStatusCmd with value: %s", retVal));
        }
        return retVal;
    }


    /**
     * This method is used when the LC firmware is 1.5.1 or older
     * 
     * @param ip
     * @param user
     * @param password
     * @param readyStateTimeout
     * @param timeout
     * @param sleepInterval
     * @return
     */
    private static boolean waitForLCReadyWithGetRSStatus(String ip, String user, String password, long readyStateTimeout, long timeout, long sleepInterval) {
        RSStatusEnum status = null;
        boolean breakOnReady = false;
        long triggerRunTime = 0;
        boolean rval = false;

        Trigger t = new Trigger(sleepInterval, timeout);
        try {
            while (t.block()) {

                GetRSStatusCmd statusCmd = new GetRSStatusCmd(ip, user, password);
                status = statusCmd.execute();
                triggerRunTime = t.getDurationInMillis();

                if (RSStatusEnum.READY.toString().equals(status.toString())) {
                    rval = true;
                    if (breakOnReady)
                        break;
                    if (triggerRunTime > readyStateTimeout)
                        break;
                } else if (RSStatusEnum.RELOADING.toString().equals(status.toString())) {
                    rval = false;
                    breakOnReady = true;
                } else if (RSStatusEnum.NOT_READY.toString().equals(status.toString()) || RSStatusEnum.ERROR.toString().equals(status.toString())) {
                    rval = false;
                }
            }
            if (status != null) {
                if (rval && breakOnReady) {
                    logger.debug(String.format("RS status transitioned to - %s - state. (%s seconds)", status.toString(), triggerRunTime / 1000));
                } else {
                    logger.debug(String.format("RS status timed out while in - %s - state. (%s seconds)", status.toString(), triggerRunTime / 1000));
                }
            }
            return rval;
        } catch (Exception e) {
            logger.error("exception in waitForLCReadyWithGetRSStatus", e);
            return false;
        } finally {
            t.cancel();
        }
    }


    /**
     * This method is used for LC version greater than 1.5.1 Method will block until one of the following occurs: Constant READY state > readyStateTimeout. Transition from
     * RELOADING to READY state. Current time > timeout while in RELOADING, NOT READY or ERROR state.
     * 
     * @param ip
     * @param user
     * @param password
     * @param timeout
     * <li>Maximum timeout in any state
     * <li>Must be in millis
     * <li>Minimum is 4 minutes
     * <li>Cannot be less than readyStateTimeout
     * @param readyStateTimeout
     * <li>timeout in ready state only
     * <li>Must be in millis
     * @return TRUE if READY, FALSE otherwise
     */
    private static boolean waitForLCReadyWithGetRSAPIStatus(String ip, String user, String password, long readyStateTimeout, long timeout, long sleepInterval) {
        RSAPIStatusEnum status = null;
        long triggerRunTime = 0;
        boolean rval = false;

        Trigger t = new Trigger(sleepInterval, timeout);
        try {
            while (t.block()) {

                GetRemoteServicesAPIStatus statusCmd = new GetRemoteServicesAPIStatus(ip, user, password);
                status = statusCmd.execute();
                triggerRunTime = t.getDurationInMillis();

                if (RSStatusEnum.READY.toString().equals(status.toString())) {
                    rval = true;
                    break;
                } else if (RSStatusEnum.NOT_READY.toString().equals(status.toString())) {
                    rval = false;
                } else if (RSStatusEnum.ERROR.toString().equals(status.toString())) {
                    rval = false;
                    break;
                }
            }
            if (status != null) {
                if (rval) {
                    logger.debug(String.format("RS API status transitioned to - %s - state. (%s seconds)", status.toString(), triggerRunTime / 1000));
                } else {
                    logger.debug(String.format("RS status timed out while in - %s - state. (%s seconds)", status.toString(), triggerRunTime / 1000));
                }
            }
            return rval;
        } catch (Exception e) {
            logger.error("exception in waitForLCReadyWithGetRSAPIStatus", e);
            return false;
        } finally {
            t.cancel();
        }
    }


    @SuppressWarnings("unchecked")
    public static int deleteAllFailedLCJobs(String ip, String user, String password) throws Exception {

        int jobsDeleted = 0;

        Thread.sleep(TEN_SEC);

        EnumerateJobs enumJobs = new EnumerateJobs(ip, 443, user, password, false);

        List<LifeCycleJob> jobList = (List<LifeCycleJob>) enumJobs.execute();
        if (jobList == null || jobList.isEmpty()) {
            return jobsDeleted;
        }

        logger.info("Deleting all failed jobs in LC job queue");

        for (LifeCycleJob job : jobList) {
            String jobStatus = job.getJobStatus();
            if (jobStatus != null && (jobStatus.equalsIgnoreCase("failed") || jobStatus.equalsIgnoreCase("completed with errors"))) {
                logger.info("Deleting Job - ID: " + job.getInstanceID() + " | Status of deleted job: " + jobStatus + " | Message: " + job.getMessage());
                deleteJob(ip, user, password, job.getInstanceID());
                jobsDeleted++;
            }
        }
        logger.debug("Deleted - " + jobsDeleted + " - jobs from LC job queue");
        return jobsDeleted;
    }


    public static int deleteJob(String ip, String user, String password, String jobID) {

        DeleteJobCmd deleteCmd = new DeleteJobCmd(ip, user, password, jobID);

        int deleteCmdReturn = 0;
        try {
            deleteCmdReturn = deleteCmd.execute().intValue();
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
        }

        return deleteCmdReturn;

    }


    public static boolean is12g(String ip, String user, String password) throws Exception {

        EnumerateSoftwareIdentityCmd cmd = new EnumerateSoftwareIdentityCmd(ip, user, password);
        List<DCIMSoftwareIdentityType> softwareIdentityList = cmd.execute();

        for (DCIMSoftwareIdentityType softwareIdentity : softwareIdentityList) {

            if (softwareIdentity.getComponentID() != null) {
                if (softwareIdentity.getComponentID().getValue().trim().equals(iDRAC_COMPONENT_ID_12G)) {
                    return true;
                }
            }
        }
        return false;
    }


    public static Object findObjectInDocument(SOAPElement doc, String xPathLocation, String namespace, QName qname) throws XPathExpressionException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        // xpath.setNamespaceContext(new PersonalNamespaceContext(namespace)); FIX ME

        XPathExpression expr = xpath.compile(xPathLocation);
        Object result = expr.evaluate(doc, qname);
        return result;

    }
    

    public static Object toJson(Document response)  {
        Element element = response.getDocumentElement();
        NodeList nodeList = element.getElementsByTagNameNS(WSCommandRNDConstant.WS_MAN_NAMESPACE, WSCommandRNDConstant.WSMAN_ITEMS_TAG);
        List<Object> jNodes = new ArrayList<Object>();
        processNodeList(nodeList, jNodes);
        return jNodes.size() == 1 ? jNodes.get(0) : jNodes;        
    }


    /**
     * @param nodeList - the dom node list to be processed
     * @param jNodes - the list to be populated with results
     */
    private static void processNodeList(NodeList nodeList, List<Object> jNodes) {
        Map<String, Object> nodeMap = new HashMap<String, Object>();
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Element.ELEMENT_NODE) {
                if (node.hasChildNodes() && node.getChildNodes().item(0).getNodeType() == Element.ELEMENT_NODE) {
                    processNodeList(node.getChildNodes(), jNodes);
                } else {
                    String key = node.getLocalName();
                    String content = node.getTextContent();
                    if(isDate(content, WSMAN_DATE_PATTERN)) {
                        content = getDateString(content, WSMAN_DATE_FORMAT);
                    }
                    if (nodeMap.containsKey(key) == false) {
                        nodeMap.put(key, StringUtils.isNumeric(content) ? Long.parseLong(content) : content);
                    } else {
                        Object o = nodeMap.get(key);
                        if(!(o instanceof List)) {
                            List<Object> valueList = new LinkedList<>();
                            valueList.add(o);
                            valueList.add(StringUtils.isNumeric(content) ? Long.parseLong(content) : content);
                            nodeMap.put(key, valueList);
                        } else {
                            List<Object> list = (List<Object>) nodeMap.get(key);
                            list.add(content);
                        }
                    }
                }
            }
        }
        if(!nodeMap.isEmpty()){
            jNodes.add(nodeMap);
        }
    }


    /**
     * Matches input string with pattern string.
     *
     * @param inputStr the input str
     * @param patternStr the pattern str
     * @return bool value
     */
    public static boolean isDate(String str, Pattern pattern) {
        if (str == null) {
            return false;
        }
        Matcher matcher = pattern.matcher(str);
        if (matcher.matches()) {
            return true;
        }
        return false;
    }


    /**
     * @param dateString
     * @param dateFormat
     * @return
     */
    private static String getDateString(String dateString, String dateFormat) {
        try{
            return DateTimeUtils.getUtcDateFromString(dateFormat, dateString).toString();
        }
        catch(Exception e)
        {
            return dateString;
        }
    }
    
}