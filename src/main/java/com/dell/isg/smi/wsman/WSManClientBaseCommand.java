/**
 * Copyright © 2017 DELL Inc. or its subsidiaries.  All Rights Reserved.
 */
package com.dell.isg.smi.wsman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableEmpty;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributablePositiveInteger;
import org.dmtf.schemas.wbem.wsman._1.wsman.EnumerationModeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.w3._2003._05.soap_envelope.Body;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;

import com.dell.isg.smi.commons.elm.exception.RuntimeCoreException;
import com.dell.isg.smi.wsman.WSManageSession.EnumReferenceParam;
import com.dell.isg.smi.wsman.command.IWSManClientCommand;
import com.dell.isg.smi.wsman.command.PersonalNamespaceContext;
import com.dell.isg.smi.wsman.command.entity.WsmanCredentials;
import com.dell.isg.smi.wsmanclient.util.WSManUtils;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.enumeration.EnumerationExtensions.Mode;

public abstract class WSManClientBaseCommand<T> implements IWSManClientCommand<T> {

    private WSManageSession WSManSession = null;

	private List<SelectorType> selectors;

    // set up a logger
    private static final Logger logger = LoggerFactory.getLogger(WSManClientBaseCommand.class);
    public static final String ENUMERATION_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/09/enumeration";
    public static final String WSMAN_WQL_NAMESPACE = "http://schemas.microsoft.com/wbem/wsman/1/WQL";


    /**
     * Original enum mappings
     *
     * SOFTWARE_INVENTORY("DCIM_SoftwareIdentity"), SOFTWARE_INSTALL_SERVICE("DCIM_SoftwareInstallationService"), SYSTEM_VIEW("DCIM_SystemView"),
     * DCIM_JOB_SERVICE("DCIM_LifecycleJob"), RAID_CONTROLLER_VIEW("DCIM_ControllerView"), RAID_VD_VIEW("DCIM_VirtualDiskView"), RAID_PD_VIEW("DCIM_PhysicalDiskView"),
     * BIOS_ENUMERATION("DCIM_BIOSEnumeration"), BOOT_SOURCE_SETTING("DCIM_BootSourceSetting"), BOOT_CONFIG_SETTTING("DCIM_BootConfigSetting"), BIOS_STRING("DCIM_BIOSString"),
     * BIOS_INTEGER("DCIM_BIOSInteger"), LIFECYCLE_JOB_CLASSNAME("DCIM_LifecycleJob"),
     *
     * OSDSVC_NAMESPACE("root/dcim/"), DCIM_JOB_SERVICE_CLASS("DCIM_JobService"), FIRMWARE_UPDATE_METHOD("InstallFromURI"), CREATE_REBOOT_METHOD("CreateRebootJob"),
     * SETUP_JOB_QUEUE("SetupJobQueue");
     */
    protected WSManClientBaseCommand() {
    };


    /**
     * Constructor
     *
     * @param ipAddr
     * @param credentials
     */
    protected WSManClientBaseCommand(String ipAddr, WsmanCredentials credentials) {

        this(ipAddr, credentials.getUserid(), credentials.getPassword(), credentials.isCertificateCheck());

    }


    /**
     * Constructor
     *
     * @param ipAddr
     * @param userName
     * @param passwd
     */
    protected WSManClientBaseCommand(String ipAddr, String userName, String passwd) {

        this(ipAddr, "443", userName, passwd, false);
    }


    protected WSManClientBaseCommand(String ipAddr, String userName, String passwd, boolean bCertCheck) {

        this(ipAddr, "443", userName, passwd, bCertCheck);
    }


    /**
     * Constructor
     *
     * @param ipAddr
     * @param userName
     * @param passwd
     * @param port
     */
    protected WSManClientBaseCommand(String ipAddr, String port, String userName, String passwd) {

        this(ipAddr, port, userName, passwd, false);

    }


    protected WSManClientBaseCommand(String ipAddr, String port, String userName, String passwd, boolean bCertCheck) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Entering Constructor: WSManBaseCommand(String ipAddr - %s, String port - %s, String userName - %s, String passwd - %s, boolean bCertCheck - %s)", ipAddr, port, userName, "####", Boolean.toString(bCertCheck)));
        }
        WSManSession = new WSManageSession(ipAddr, userName, passwd, port, bCertCheck);
        WSManSession.owningCmd = this.getClass().getName();
        logger.info("Firing command " + WSManSession.owningCmd);

        logger.trace("Exiting Constructor: WSManBaseCommand()");
    }


    protected WSManageSession getSession() {

        return WSManSession;
    }


    protected void setUserParam(String key, String value) {

        getSession().addUserParam(key, value);

    }


    public abstract Object execute() throws Exception;

    // FIXME: Attempt to factor out the following, or at least enum it
    // protected static String getInstanceId(String updateType) {
    // String type = updateType.toLowerCase();
    // if (type.equals("bios")) {
    // return "DCIM:INSTALLED:NONPCI:159:1.3.4";
    // } else if (type.equals("raid")) {
    // return "DCIM:INSTALLED:NONPCI:159:6.22.00";
    // } else if (type.equals("nic") || type.equals("lom")) {
    // return "DCIM:INSTALLED:PCI:14E4:1639:0237:1028:5.0.10";
    // } else if (type.equals("firmware")) {
    // return "";
    // } else if (type.equals("drvpk")) {
    // return "DCIM:INSTALLED:NONPCI:18981:5111.1";
    // } else if (type.equals("diags")) {
    // return "DCIM:INSTALLED:NONPCI:196:5116.1";
    // } else if (type.equals("perc6e")) {
    // return "DCIM:INSTALLED:PCI:1000:0060:1F0A:1028:06.20.00.13";
    // } else if (type.equals("perc6i")) {
    // return "DCIM:INSTALLED:PCI:1000:0060:1F0C:1028:06.20.00.13";
    // } else if (type.equals("h700a")) {
    // return "DCIM:INSTALLED:PCI:1000:0079:1F16:1028:12.01.00.53";
    // } else if (type.equals("h800a")) {
    // return "DCIM:INSTALLED:PCI:1000:0079:1F15:1028:12.01.00.53";
    // } else if (type.equals("idrac")) {
    // return "DCIM:INSTALLED:NONPCI:15051:1.30";
    // } else if (type.equals("idracmono")) {
    // return "DCIM:INSTALLED:NONPCI:20137:1.30";
    // } else if (type.equals("sas")) {
    // return "DCIM:INSTALLED:PCI:1000:0058:1F10:1028:06.22.02";
    // } else if (type.equals("psu")) {
    // return "DCIM:INSTALLED:NONPCI:1750:3.02.50";
    // } else if (type.equals("usc")) {
    // return "DCIM:INSTALLED:NONPCI:18980:1.30.60";
    // }
    //
    // else {
    // throw new IllegalArgumentException("Update Type: '" + updateType
    // + "' is not supported!");
    // }
    // }

    protected NodeList sendRequestEnumerationReturnNodeList() throws Exception {

        Document response = sendRequestEnumerationReturnDocument();
        Element element = response.getDocumentElement();
        NodeList nodeList = element.getElementsByTagNameNS(WSCommandRNDConstant.WS_MAN_NAMESPACE, WSCommandRNDConstant.WSMAN_ITEMS_TAG);

        return nodeList;

    }

    protected Object sendRequestEnumerationReturnJson() throws Exception {
        Document tempDoc = WSManUtils.toDocument(getSession().sendRequestEnumerationGetXml(null));
        return WSManUtilities.toJson(tempDoc);
    }
    
    public T parse(String xml) throws Exception {
        Document tempDoc = WSManUtils.toDocument(xml);
        return (T) WSManUtilities.toJson(tempDoc);
    }

    protected org.w3c.dom.Document sendRequestEnumerationReturnDocument() throws Exception {
        Addressing retAddressing = getSession().sendRequestEnumeration();
        return getSession().extractAddressBody(retAddressing);
    }


    protected org.w3c.dom.Document sendRequestEnumerationReturnDocument(Mode enumMode) throws Exception {

        Addressing retAddressing = getSession().sendRequestEnumeration(enumMode);
        return getSession().extractAddressBody(retAddressing);
    }


    protected Enumeration sendRequestEnumeration() throws Exception {
        return new Enumeration(getSession().sendRequestEnumeration());
    }


    protected Enumeration sendRequestEnumeration(Mode enumMode) throws SOAPException, JAXBException, DatatypeConfigurationException, IOException, XPathExpressionException {
        return new Enumeration(getSession().sendRequestEnumeration(enumMode));
    }

    public enum WSManClassEnum {

        DCIM_SoftwareIdentity, DCIM_SoftwareInstallationService, DCIM_SoftwareUpdateConcreteJob, DCIM_SystemView, DCIM_ControllerView, DCIM_VirtualDiskView, DCIM_PhysicalDiskView, DCIM_BIOSEnumeration, DCIM_BootSourceSetting, DCIM_BootConfigSetting, DCIM_BIOSString, DCIM_BIOSInteger, DCIM_NICView, DCIM_LifecycleJob, DCIM_JobService, InstallFromURI, CreateRebootJob, SetupJobQueue, DCIM_ComputerSystem, DCIM_OSDeploymentService, DCIM_BIOSService, DCIM_OEM_DataAccessModule, DCIM_RAIDService, CIM_ComputerSystem, DCIM_LCService, DCIM_IDRACCardView, DCIM_SPComputerSystem, CIM_IPProtocolEndpoint, CIM_Chassis, CIM_SoftwareIdentity, CIM_InstalledSoftwareIdentity, DCIM_Sellogentry, DCIM_Memoryview, DCIM_Powersupplyview, DCIM_PCIDeviceView, DCIM_CPUView, DCIM_EnclosureView, DCIM_iDRACCardAttribute, DCIM_PSNumericsensor, DCIM_iDRACCardString, DCIM_iDRACCardEnumeration, DCIM_NICStatistics, DCIM_NICCapabilities, DCIM_BaseMetricValue, DCIM_AggregationMetricValue, CIM_Account, DCIM_LicenseManagementService, DCIM_LicensableDevice, DCIM_SelRecordLog, DCIM_License, DCIM_SystemManagementService, DCIM_ModularChassisView, DCIM_BladeServerView, CIM_PhysicalElement, DCIM_FanView, DCIM_VFlashView, DCIM_Sensor, DCIM_NumericSensor, DCIM_ControllerBatteryView, DCIM_PCIeSSDView, DCIM_NICEnumeration, DCIM_NICString, DCIM_NICInteger, DCIM_RAIDEnumeration, DCIM_RAIDString, DCIM_RAIDInteger, DCIM_SystemString, DCIM_LCLogEntry;
    }

    public enum WSManMethodParamEnum {

        INSTANCE_ID("InstanceID"), SOURCE("source"), ENABLED_STATE("EnabledState"), REBOOT_JOB_TYPE("RebootJobType"), SCHEDULED_START_TIME("ScheduledStartTime"), SYSTEM_CLASS_NAME("SystemCreationClassName"), CREATION_CLASS_NAME("CreationClassName"), SYSTEM_NAME("SystemName"), NAME("Name"), TARGET("Target"), REBOOT_IF_REQUIRED("RebootIfRequired"), REQUESTED_STATE("RequestedState"), TIME_OUT_PERIOD("TimeoutPeriod"), ATTRIBUTE_NAME("AttributeName"), ATTRIBUTE_VALUE("AttributeValue"), PROVISIONING_SERVER("ProvisioningServer"), RESET_TO_FACTORY_DEFAULTS("ResetToFactoryDefaults"), PERFORM_AUTO_DISCOVERY("PerformAutoDiscovery");

        String enumValue;


        WSManMethodParamEnum(String value) {
            enumValue = value;
        }


        @Override
        public String toString() {
            return enumValue;
        }
    }

    public enum WSManMethodEnum {
        DRIVER_PACK_INFO("GetDriverPackInfo"), HOST_MAC_INFO("GetHostMACInfo"), BOOT_TO_PXE("BootToPXE"), BOOT_REMOTE_ISO("BootToNetworkISO"), UNPACK_AND_ATTACH("UnpackAndAttach"), DETACH_ISO("DetachISOImage"), DETACH_DRIVERS("DetachDrivers"), DETACH_ISO_VFLASH("DetachISOFromVFlash"), DELETE_ISO_VFLASH("DeleteISOFromVFlash"), DOWNLOAD_ISO_VFLASH("DownloadISOToVFlash"), BOOT_ISO_VFLASH("BootToISOFromVFlash"), CREATE_REBOOT_JOB("CreateRebootJob"), DELETE_JOB_QUEUE("DeleteJobQueue"), SETUP_JOB_QUEUE("SetupJobQueue"), INSTALL_FROM_URI("InstallFromURI"), CHANGE_BOOT_ORDER("ChangeBootOrderByInstanceID"), CHANGE_BOOT_STATUS("ChangeBootSourceState"), CREATE_TARGET_CONFIG_JOB("CreateTargetedConfigJob"), CREATE_LC_SERVICE_CONFIG_JOB("CreateConfigJob"), SET_ATTRIBUTE("SetAttribute"), SET_ATTRIBUTES("SetAttributes"), SEND_CMD("SendCmd"), REQUESTED_STATE_CHANGE("RequestStateChange"), REINITIATE_DHS("ReInitiateDHS"), GET_RS_STATUS("GetRSStatus"), GET_REMOTE_SERVICES_API_STATUS("GetRemoteServicesAPIStatus"), GET_NETWORK_ISO_IMAGE_CONNECTION_INFO("GetNetworkISOImageConnectionInfo"), CONNECT_TO_NEWORK_ISO_IMAGE("ConnectNetworkISOImage"), DISCONNECT_NETWORK_ISO_IMAGE("DisconnectNetworkISOImage"), EXPORT_LICENSE("ExportLicense"), CLEAR_SEL_LOG("ClearLog"), BLINK_LED("IdentifyChassis");
        String enumValue;


        WSManMethodEnum(String value) {
            enumValue = value;
        }


        @Override
        public String toString() {
            return enumValue;
        }

    }


    /*
     * utility function to convert Java String into XML
     */
    protected Document convertStringToXMLDocument(String xmlSource) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document document;

        builder = factory.newDocumentBuilder();

        document = builder.parse(new InputSource(new StringReader(xmlSource)));
        return document;

    }


    // protected so only WSManBaseCommand Children can use this to aid with
    // testing.
    protected static Document callCmdLine(String cmdLine) {
        Document retDoc = null;
        String lineRead = null;
        DocumentBuilderFactory factory;
        DocumentBuilder builder = null;

        StringBuilder cmdSb = new StringBuilder("cmd //c \"");
        cmdSb.append(cmdLine);
        cmdSb.append("\"");
        try {

            // Process p=Runtime.getRuntime().exec(cmdSb.toString());
            Process p = Runtime.getRuntime().exec(cmdLine);
            p.waitFor();
            // Thread.sleep(10);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            lineRead = reader.readLine();

            factory = DocumentBuilderFactory.newInstance();
            // factory.setNamespaceAware(true);
            // factory.setValidating(true);
            try {
                builder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                // LoggerFactory.getLogger(WSManBaseCommand.class.getName()).log(
                // Level.SEVERE, null, ex);
                Marker fatal = MarkerFactory.getMarker("FATAL");
                LoggerFactory.getLogger(WSManClientBaseCommand.class).error(fatal, ex.getMessage(), ex);
            }
            InputStream xmlStream = p.getInputStream();
            try {
                if (builder != null)
                    retDoc = builder.parse(xmlStream);
                /*
                 * while(lineRead==null) { System.out.println(lineRead); lineRead=reader.readLine(); } *
                 */
            } catch (SAXException ex) {
                // LoggerFactory.getLogger(WSManBaseCommand.class.getName()).log(
                // Level.SEVERE, null, ex);
                Marker fatal = MarkerFactory.getMarker("FATAL");
                LoggerFactory.getLogger(WSManClientBaseCommand.class).error(fatal, ex.getMessage(), ex);
            }

        } catch (InterruptedException ex) {
            // LoggerFactory.getLogger(WSManBaseCommand.class.getName()).log(
            // Level.SEVERE, null, ex);
            Marker fatal = MarkerFactory.getMarker("FATAL");
            LoggerFactory.getLogger(WSManClientBaseCommand.class).error(fatal, ex.getMessage(), ex);

        } catch (IOException e1) {
            // System.out.println("Error is: " + e1.getMessage());
            Marker fatal = MarkerFactory.getMarker("FATAL");
            LoggerFactory.getLogger(WSManClientBaseCommand.class).error(fatal, e1.getMessage(), e1);
        }

        // System.out.println("Done");
        return retDoc;
    }


    protected List<EnumReferenceParam> sendRequestEnumerationWithEPR(Mode enumMode) throws Exception {

        Addressing addressing = getSession().sendRequestEnumeration(enumMode);

        // convert the addressing to EnumReferenceParam response.
        return getSession().extractRequestEnumerationRefParameters(new Enumeration(addressing));

    }


    protected void addSelectors(String resourceURI) {

        try {

            if (StringUtils.isEmpty(this.WSManSession.getResourceUri()) || StringUtils.isBlank(this.WSManSession.getResourceUri())) {
                this.WSManSession.setResourceUri(resourceURI);
            }

            List<EnumReferenceParam> enumRefParams = this.sendRequestEnumerationWithEPR(Mode.EnumerateEPR);
            this.WSManSession.getSelectors().addAll(enumRefParams.get(0).getSelectorSetTypes().getSelector());

        } catch (Exception e) {
            // log.error("",e);
            throw new RuntimeCoreException(e);
        }
    }


    /*
     * public static void printToSysOut(Document doc ) { System.out.println( "Doc is: ");
     *
     * Writer writer = new OutputStreamWriter( System.out ); OutputFormat format = new OutputFormat(doc); format.setLineWidth(65); format.setIndenting(true); format.setIndent(2);
     * XMLSerializer serializer = new XMLSerializer(writer, format); try { serializer.serialize(doc); } catch (IOException e) { // Auto-generated catch block e.printStackTrace(); }
     * }
     */

    public Document getlastRequestBody() {
        return getSession().getLastRequestBody();
    }


    public Addressing getlastAddressingResponse() {
        return getSession().getlastAddressingResponse();
    }


    public Addressing getlastAddressingRequest() {
        return getSession().getlastAddressingRequest();
    }


    public void resetSession(String destIP, String username, String password, String port) {
        getSession().resetSession(destIP, username, password, port);
    }


    protected Logger getLogger(Class c) {
        return LoggerFactory.getLogger(c);
    }


    /**
     * Set Read timeout (in seconds) for this http request
     *
     * @param to
     */
    protected void setTimeoutInSeconds(int to) {
        getSession().setTimeoutInSeconds(to);
    }


    /**
     * Add filters to enumeration request
     *
     * @param name
     * @param value
     */
    public void addEnumerateFilter(String name, String value) {
        getSession().addFilter(name, value);
    }


    protected Object findObjectInDocument(SOAPElement doc, String xPathLocation, String namespace, QName qname) throws XPathExpressionException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new PersonalNamespaceContext(namespace));

        XPathExpression expr = xpath.compile(xPathLocation);
        Object result = expr.evaluate(doc, qname);
        return result;
    }


    /**
     * <li>This method calls utility method WSManUtilities.waitForLCReadyStatus using current session credentials
     *
     * @param timeout
     * <li>Maximum timeout in any state
     * <li>Must be in millis
     * <li>Minimum is 4 minutes
     * @return TRUE if READY state, FALSE otherwise
     * @see {@link WSManUtilities#waitForLCReadyStatus(String, String, String, long)}
     */
    public boolean waitForReadyStatus(long timeout) {
        return WSManUtilities.waitForLCReadyStatus(WSManSession.getIpAddress(), WSManSession.getUser(), WSManSession.getPassword(), timeout);
    }


    /**
     * <li>This method calls utility method WSManUtilities.waitForLCReadyStatus using current session credentials
     *
     * @param timeout
     * <li>Maximum timeout in any state
     * <li>Must be in millis
     * <li>Cannot be less than readyStateTimeout
     * @param readyStateTimeout
     * <li>timeout in ready state only
     * <li>Must be in millis
     * @return TRUE if READY state, FALSE otherwise
     * @see {@link WSManUtilities#waitForLCReadyStatus(String, String, String, long, long)}
     */
    public boolean waitForReadyStatus(long timeout, long readyStateTimeout) {
        return WSManUtilities.waitForLCReadyStatus(WSManSession.getIpAddress(), WSManSession.getUser(), WSManSession.getPassword(), timeout);
    }


    /**
     * <li>This method calls utility method WSManUtilities.waitForLCReadyStatusPostCSIOR using current session credentials
     *
     * @param timeout
     * <li>Maximum timeout in any state
     * <li>Must be in millis
     * <li>Minimum is 4 minutes
     * @return TRUE if READY state, FALSE otherwise
     * @see {@link WSManUtilities#waitForLCReadyStatusPostCSIOR(String, String, String, long)}
     */
    public boolean waitForReadyStatusPostCSIOR(long timeout) {
        return WSManUtilities.waitForLCReadyStatusPostCSIOR(WSManSession.getIpAddress(), WSManSession.getUser(), WSManSession.getPassword(), timeout);
    }


    public String getLCErrorCode() {
        return this.getSession().getLCMessageID();
    }


    public String getLCErrorStr() {
        return this.getSession().getLCMessageStr();
    }


    // public String getTimeout() {
    // return "PT60.001S";
    // }

    public String getAction() {
        return ENUMERATION_NAMESPACE + "/Enumerate";
    }


    /**
     * Returns the enumeration mode for the request. The default mode, {@code EnumerateObject} causes only the resource instance items XML to be returned. Subclasses may override
     * this method and return {@code EnumerateObjectAndEPR} which will cause the returned XML items to include both the resource instance XML and the corresponding
     * EndpointReference that may be used to address the resource instance in later {@code Get} or {@code Put} commands.
     *
     * @return The enumeration request mode
     */
    protected EnumerationModeType getMode() {
        return null;
    }


    public String getFilterWQL() {
        return null;
    }


    /**
     * The maximum number of resource instances that should be returned for this enumeration request. Subclasses should override this method in order to change the default,
     * {@code 256L}.
     *
     * @return The maximum number of resource instances to return.
     */
    public long getMaxElements() {
        return 2048L;
    }


    public Body getBody() {
        org.w3._2003._05.soap_envelope.ObjectFactory env = new org.w3._2003._05.soap_envelope.ObjectFactory();
        org.dmtf.schemas.wbem.wsman._1.wsman.ObjectFactory wsman = new org.dmtf.schemas.wbem.wsman._1.wsman.ObjectFactory();
        org.xmlsoap.schemas.ws._2004._09.enumeration.ObjectFactory wsen = new org.xmlsoap.schemas.ws._2004._09.enumeration.ObjectFactory();

        Body body = env.createBody();
        
        Enumerate enumerate = wsen.createEnumerate();
        body.getAny().add(enumerate);

        // default mode is enumerate. getMode is used to change to a different mode
        EnumerationModeType mode = getMode();
        if (null != mode) {
            switch (getMode()) {
            case ENUMERATE_EPR:
                enumerate.getAny().add(wsman.createEnumerationMode(EnumerationModeType.ENUMERATE_EPR));
                break;
            case ENUMERATE_OBJECT_AND_EPR:
                enumerate.getAny().add(wsman.createEnumerationMode(EnumerationModeType.ENUMERATE_OBJECT_AND_EPR));
                break;
            default:
                break; // default, do nothing
            }
        }

        AttributableEmpty empty = wsman.createAttributableEmpty();
        JAXBElement<AttributableEmpty> optimizeEnumeration = wsman.createOptimizeEnumeration(empty);
        enumerate.getAny().add(optimizeEnumeration);

        AttributablePositiveInteger max = wsman.createAttributablePositiveInteger();
        max.setValue(BigInteger.valueOf(getMaxElements()));
        JAXBElement<AttributablePositiveInteger> maxElements = wsman.createMaxElements(max);
        enumerate.getAny().add(maxElements);

        /*
         * if (getFilterWQL() != null) { FilterMixedDataType filterDataType = wsman.createFilterMixedDataType(); filterDataType.setDialect(WSMAN_WQL_NAMESPACE);
         * filterDataType.getContent().add(getFilterWQL()); JAXBElement<FilterMixedDataType> filter = wsman.createFilter(filterDataType); enumerate.getAny().add(filter); }
         */

        return body;
    }


    public String getResourceURI() {
        throw new NotImplementedException("Not Implemented");
    }

	public List<SelectorType> getSelectors() {
		return selectors;
	}


	public void setSelectors(List<SelectorType> selectors) {
		this.selectors = selectors;
	}

    /**
     * Add Selector with this name/value pair
     *
     * @param key
     * @param value
     */
    public void addSelector(String key, String value) {
    	if(selectors == null){
    		selectors = new ArrayList<SelectorType>();
    	}
        SelectorType sel = new SelectorType();
        sel.setName(key);
        sel.getContent().add(value);
        selectors.add(sel);
    }

}
