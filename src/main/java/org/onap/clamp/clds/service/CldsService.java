/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 * 
 */

package org.onap.clamp.clds.service;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.TransformerException;

import org.apache.camel.Produce;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.parser.ParseException;
import org.onap.clamp.clds.camel.CamelProxy;
import org.onap.clamp.clds.client.DcaeDispatcherServices;
import org.onap.clamp.clds.client.DcaeInventoryServices;
import org.onap.clamp.clds.client.req.sdc.SdcCatalogServices;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.exception.CldsConfigException;
import org.onap.clamp.clds.exception.policy.PolicyClientException;
import org.onap.clamp.clds.exception.sdc.SdcCommunicationException;
import org.onap.clamp.clds.model.CldsDbServiceCache;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsHealthCheck;
import org.onap.clamp.clds.model.CldsInfo;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.CldsModelProp;
import org.onap.clamp.clds.model.CldsMonitoringDetails;
import org.onap.clamp.clds.model.CldsServiceData;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.model.DcaeEvent;
import org.onap.clamp.clds.model.ValueItem;
import org.onap.clamp.clds.model.properties.AbstractModelElement;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.sdc.SdcResource;
import org.onap.clamp.clds.model.sdc.SdcServiceDetail;
import org.onap.clamp.clds.model.sdc.SdcServiceInfo;
import org.onap.clamp.clds.sdc.controller.installer.CsarInstallerImpl;
import org.onap.clamp.clds.transform.XslTransformer;
import org.onap.clamp.clds.util.JacksonUtils;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Service to save and retrieve the CLDS model attributes.
 */
@Component
@Path("/clds")
public class CldsService extends SecureServiceBase {

    @Produce(uri = "direct:processSubmit")
    private CamelProxy camelProxy;
    protected static final EELFLogger securityLogger = EELFManager.getInstance().getSecurityLogger();
    private static final String RESOURCE_NAME = "clds-version.properties";
    public static final String GLOBAL_PROPERTIES_KEY = "files.globalProperties";
    @Value("${clamp.config.security.permission.type.cl:permission-type-cl}")
    private String cldsPersmissionTypeCl;
    @Value("${clamp.config.security.permission.type.cl.manage:permission-type-cl-manage}")
    private String cldsPermissionTypeClManage;
    @Value("${clamp.config.security.permission.type.cl.event:permission-type-cl-event}")
    private String cldsPermissionTypeClEvent;
    @Value("${clamp.config.security.permission.type.filter.vf:permission-type-filter-vf}")
    private String cldsPermissionTypeFilterVf;
    @Value("${clamp.config.security.permission.type.template:permission-type-template}")
    private String cldsPermissionTypeTemplate;
    @Value("${clamp.config.security.permission.instance:dev}")
    private String cldsPermissionInstance;
    private SecureServicePermission permissionReadCl;
    private SecureServicePermission permissionUpdateCl;
    private SecureServicePermission permissionReadTemplate;
    private SecureServicePermission permissionUpdateTemplate;

    @PostConstruct
    private final void afterConstruction() {
        permissionReadCl = SecureServicePermission.create(cldsPersmissionTypeCl, cldsPermissionInstance, "read");
        permissionUpdateCl = SecureServicePermission.create(cldsPersmissionTypeCl, cldsPermissionInstance, "update");
        permissionReadTemplate = SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance,
                "read");
        permissionUpdateTemplate = SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance,
                "update");
    }

    @Autowired
    private CldsDao cldsDao;
    @Autowired
    private XslTransformer cldsBpmnTransformer;
    @Autowired
    private ClampProperties refProp;
    @Autowired
    private SdcCatalogServices sdcCatalogServices;
    @Autowired
    private DcaeDispatcherServices dcaeDispatcherServices;
    @Autowired
    private DcaeInventoryServices dcaeInventoryServices;

    /*
     * @return list of CLDS-Monitoring-Details: CLOSELOOP_NAME | Close loop name
     * used in the CLDS application (prefix: ClosedLoop- + unique ClosedLoop ID)
     * MODEL_NAME | Model Name in CLDS application SERVICE_TYPE_ID | TypeId
     * returned from the DCAE application when the ClosedLoop is submitted
     * (DCAEServiceTypeRequest generated in DCAE application). DEPLOYMENT_ID |
     * Id generated when the ClosedLoop is deployed in DCAE. TEMPLATE_NAME |
     * Template used to generate the ClosedLoop model. ACTION_CD | Current state
     * of the ClosedLoop in CLDS application.
     */
    @GET
    @Path("/cldsDetails")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CldsMonitoringDetails> getCLDSDetails() {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: GET model details", getPrincipalName());
        List<CldsMonitoringDetails> cldsMonitoringDetailsList = cldsDao.getCLDSMonitoringDetails();
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get cldsDetails success", this.getClass().getName());
        auditLogger.info("GET cldsDetails completed");
        return cldsMonitoringDetailsList;
    }

    /*
     * CLDS IFO service will return 3 things 1. User Name 2. CLDS code version
     * that is currently installed from pom.xml file 3. User permissions
     */
    @GET
    @Path("/cldsInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public CldsInfo getCldsInfo() {
        CldsInfo cldsInfo = new CldsInfo();
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: GET cldsInfo", getPrincipalName());
        LoggingUtils.setTimeContext(startTime, new Date());
        // Get the user info
        cldsInfo.setUserName(getUserName());
        // Get CLDS application version
        String cldsVersion = "";
        Properties props = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream resourceStream = loader.getResourceAsStream(RESOURCE_NAME)) {
            props.load(resourceStream);
            cldsVersion = props.getProperty("clds.version");
        } catch (Exception ex) {
            logger.error("Exception caught during the clds.version reading", ex);
        }
        cldsInfo.setCldsVersion(cldsVersion);
        // Get the user list of permissions
        cldsInfo.setPermissionReadCl(isAuthorizedNoException(permissionReadCl));
        cldsInfo.setPermissionUpdateCl(isAuthorizedNoException(permissionUpdateCl));
        cldsInfo.setPermissionReadTemplate(isAuthorizedNoException(permissionReadTemplate));
        cldsInfo.setPermissionUpdateTemplate(isAuthorizedNoException(permissionUpdateTemplate));
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get cldsInfo success", this.getClass().getName());
        securityLogger.info("GET cldsInfo completed");
        return cldsInfo;
    }

    /**
     * REST service that retrieves clds healthcheck information.
     *
     * @return CldsHealthCheck class containing healthcheck info
     */
    @GET
    @Path("/healthcheck")
    @Produces(MediaType.APPLICATION_JSON)
    public Response gethealthcheck() {
        CldsHealthCheck cldsHealthCheck = new CldsHealthCheck();
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: GET healthcheck", "Clamp-Health-Check");
        LoggingUtils.setTimeContext(startTime, new Date());
        boolean healthcheckFailed = false;
        try {
            cldsDao.doHealthCheck();
            cldsHealthCheck.setHealthCheckComponent("CLDS-APP");
            cldsHealthCheck.setHealthCheckStatus("UP");
            cldsHealthCheck.setDescription("OK");
            LoggingUtils.setResponseContext("0", "Get healthcheck success", this.getClass().getName());
        } catch (Exception e) {
            healthcheckFailed = true;
            logger.error("CLAMP application DB Error", e);
            LoggingUtils.setResponseContext("999", "Get healthcheck failed", this.getClass().getName());
            cldsHealthCheck.setHealthCheckComponent("CLDS-APP");
            cldsHealthCheck.setHealthCheckStatus("DOWN");
            cldsHealthCheck.setDescription("NOT-OK");
        }
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        logger.info("GET healthcheck completed");
        if (healthcheckFailed) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(cldsHealthCheck).build();
        } else {
            return Response.status(Response.Status.OK).entity(cldsHealthCheck).build();
        }
    }

    /**
     * REST service that retrieves BPMN for a CLDS model name from the database.
     * This is subset of the json getModel. This is only expected to be used for
     * testing purposes, not by the UI.
     *
     * @param modelName
     * @return bpmn xml text - content of bpmn given name
     */
    @GET
    @Path("/model/bpmn/{modelName}")
    @Produces(MediaType.TEXT_XML)
    public String getBpmnXml(@PathParam("modelName") String modelName) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: GET model bpmn", getPrincipalName());
        isAuthorized(permissionReadCl);
        logger.info("GET bpmnText for modelName={}", modelName);
        CldsModel model = CldsModel.retrieve(cldsDao, modelName, false);
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get model bpmn success", this.getClass().getName());
        auditLogger.info("GET model bpmn completed");
        return model.getBpmnText();
    }

    /**
     * REST service that retrieves image for a CLDS model name from the
     * database. This is subset of the json getModel. This is only expected to
     * be used for testing purposes, not by the UI.
     *
     * @param modelName
     * @return image xml text - content of image given name
     */
    @GET
    @Path("/model/image/{modelName}")
    @Produces(MediaType.TEXT_XML)
    public String getImageXml(@PathParam("modelName") String modelName) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: GET model image", getPrincipalName());
        isAuthorized(permissionReadCl);
        logger.info("GET imageText for modelName={}", modelName);
        CldsModel model = CldsModel.retrieve(cldsDao, modelName, false);
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get model image success", this.getClass().getName());
        auditLogger.info("GET model image completed");
        return model.getImageText();
    }

    /**
     * REST service that retrieves a CLDS model by name from the database.
     *
     * @param modelName
     * @return clds model - clds model for the given model name
     */
    @GET
    @Path("/model/{modelName}")
    @Produces(MediaType.APPLICATION_JSON)
    public CldsModel getModel(@PathParam("modelName") String modelName) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: GET model", getPrincipalName());
        isAuthorized(permissionReadCl);
        logger.debug("GET model for  modelName={}", modelName);
        CldsModel cldsModel = CldsModel.retrieve(cldsDao, modelName, false);
        isAuthorizedForVf(cldsModel);
        // Checking condition whether our CLDS model can call Inventory Method
        if (cldsModel.canInventoryCall()) {
            try {
                // Method to call dcae inventory and invoke insert event method
                dcaeInventoryServices.setEventInventory(cldsModel, getUserId());
            } catch (Exception e) {
                LoggingUtils.setErrorContext("900", "Set event inventory error");
                logger.error("getModel set event Inventory error:" + e);
            }
        }
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get model success", this.getClass().getName());
        auditLogger.info("GET model completed");
        return cldsModel;
    }

    /**
     * REST service that saves a CLDS model by name in the database.
     *
     * @param modelName
     */
    @PUT
    @Path("/model/{modelName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CldsModel putModel(@PathParam("modelName") String modelName, CldsModel cldsModel) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: PUT model", getPrincipalName());
        isAuthorized(permissionUpdateCl);
        isAuthorizedForVf(cldsModel);
        logger.info("PUT model for  modelName={}", modelName);
        logger.info("PUT bpmnText={}", cldsModel.getBpmnText());
        logger.info("PUT propText={}", cldsModel.getPropText());
        logger.info("PUT imageText={}", cldsModel.getImageText());
        cldsModel.setName(modelName);
        if (cldsModel.getTemplateName() != null) {
            CldsTemplate template = cldsDao.getTemplate(cldsModel.getTemplateName());
            if (template != null) {
                cldsModel.setTemplateId(template.getId());
                cldsModel.setDocText(template.getPropText());
                // This is to provide the Bpmn XML when Template part in UI is
                // disabled
                cldsModel.setBpmnText(template.getBpmnText());
            }
        }
        cldsModel.save(cldsDao, getUserId());
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Put model success", this.getClass().getName());
        auditLogger.info("PUT model completed");
        return cldsModel;
    }

    /**
     * REST service that retrieves a list of CLDS model names.
     *
     * @return model names in JSON
     */
    @GET
    @Path("/model-names")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ValueItem> getModelNames() {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: GET model names", getPrincipalName());
        isAuthorized(permissionReadCl);
        logger.info("GET list of model names");
        List<ValueItem> names = cldsDao.getBpmnNames();
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get model names success", this.getClass().getName());
        auditLogger.info("GET model names completed");
        return names;
    }

    private void fillInCldsModel(CldsModel model) {
        if (model.getTemplateName() != null) {
            CldsTemplate template = cldsDao.getTemplate(model.getTemplateName());
            if (template != null) {
                model.setTemplateId(template.getId());
                model.setDocText(template.getPropText());
                // This is to provide the Bpmn XML when Template part in UI
                // is
                // disabled
                model.setBpmnText(template.getBpmnText());
            }
        }
    }

    /**
     * REST service that saves and processes an action for a CLDS model by name.
     *
     * @param action
     * @param modelName
     * @param test
     * @param model
     * @return
     * @throws TransformerException
     *             In case of issues when doing the XSLT of the BPMN flow
     * @throws ParseException
     *             In case of issues when parsing the JSON
     * @throws GeneralSecurityException
     *             In case of issues when decrypting the password
     * @throws DecoderException
     *             In case of issues with the Hex String decoding
     */
    @PUT
    @Path("/action/{action}/{modelName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putModelAndProcessAction(@PathParam("action") String action,
            @PathParam("modelName") String modelName, @QueryParam("test") String test, CldsModel model)
            throws TransformerException, ParseException {
        Date startTime = new Date();
        CldsModel retrievedModel = null;
        Boolean errorCase = false;
        try {
            LoggingUtils.setRequestContext("CldsService: Process model action", getPrincipalName());
            String actionCd = action.toUpperCase();
            SecureServicePermission permisionManage = SecureServicePermission.create(cldsPermissionTypeClManage,
                    cldsPermissionInstance, actionCd);
            isAuthorized(permisionManage);
            isAuthorizedForVf(model);
            String userId = getUserId();
            logger.info("PUT actionCd={}", actionCd);
            logger.info("PUT modelName={}", modelName);
            logger.info("PUT test={}", test);
            logger.info("PUT bpmnText={}", model.getBpmnText());
            logger.info("PUT propText={}", model.getPropText());
            logger.info("PUT userId={}", userId);
            logger.info("PUT getTypeId={}", model.getTypeId());
            logger.info("PUT deploymentId={}", model.getDeploymentId());
            this.fillInCldsModel(model);
            // save model to db
            model.setName(modelName);
            model.save(cldsDao, getUserId());
            // get vars and format if necessary
            String prop = model.getPropText();
            String bpmn = model.getBpmnText();
            String docText = model.getDocText();
            String controlName = model.getControlName();
            String bpmnJson = cldsBpmnTransformer.doXslTransformToString(bpmn);
            logger.info("PUT bpmnJson={}", bpmnJson);
            // Flag indicates whether it is triggered by Validation Test button
            // from
            // UI
            boolean isTest = false;
            if (test != null && test.equalsIgnoreCase("true")) {
                isTest = true;
            } else {
                String actionTestOverride = refProp.getStringValue("action.test.override");
                if (actionTestOverride != null && actionTestOverride.equalsIgnoreCase("true")) {
                    logger.info("PUT actionTestOverride={}", actionTestOverride);
                    logger.info("PUT override test indicator and setting it to true");
                    isTest = true;
                }
            }
            logger.info("PUT isTest={}", isTest);
            boolean isInsertTestEvent = false;
            String insertTestEvent = refProp.getStringValue("action.insert.test.event");
            if (insertTestEvent != null && insertTestEvent.equalsIgnoreCase("true")) {
                isInsertTestEvent = true;
            }
            logger.info("PUT isInsertTestEvent={}", isInsertTestEvent);
            // determine if requested action is permitted
            model.validateAction(actionCd);
            logger.info("modelProp - " + prop);
            logger.info("docText - " + docText);
            try {
                String result = camelProxy.submit(actionCd, prop, bpmnJson, modelName, controlName, docText, isTest,
                        userId, isInsertTestEvent);
                logger.info("Starting Camel flow on request, result is: ", result);
            } catch (SdcCommunicationException | PolicyClientException | BadRequestException e) {
                errorCase = true;
                logger.error("Exception occured during invoking Camel process", e);
            }
            // refresh model info from db (get fresh event info)
            retrievedModel = CldsModel.retrieve(cldsDao, modelName, false);
            if (!isTest && (actionCd.equalsIgnoreCase(CldsEvent.ACTION_SUBMIT)
                    || actionCd.equalsIgnoreCase(CldsEvent.ACTION_RESUBMIT)
                    || actionCd.equalsIgnoreCase(CldsEvent.ACTION_SUBMITDCAE))) {
                if (retrievedModel.getTemplateName().startsWith(CsarInstallerImpl.TEMPLATE_NAME_PREFIX)) {
                    // SDC artifact case
                    logger.info("Skipping DCAE inventory call as closed loop has been created from SDC notification");
                    DcaeEvent dcaeEvent = new DcaeEvent();
                    dcaeEvent.setArtifactName(retrievedModel.getControlName() + ".yml");
                    dcaeEvent.setEvent(DcaeEvent.EVENT_DISTRIBUTION);
                    CldsEvent.insEvent(cldsDao, dcaeEvent.getControlName(), userId, dcaeEvent.getCldsActionCd(),
                            CldsEvent.ACTION_STATE_RECEIVED, null);
                } else {
                    // This should be done only when the call to DCAE
                    // has not yet been done. When CL comes from SDC
                    // this is not required as the DCAE inventory call is done
                    // during the CL deployment.
                    dcaeInventoryServices.setEventInventory(retrievedModel, getUserId());
                }
                retrievedModel.save(cldsDao, getUserId());
            }
            // audit log
            LoggingUtils.setTimeContext(startTime, new Date());
            LoggingUtils.setResponseContext("0", "Process model action success", this.getClass().getName());
            auditLogger.info("Process model action completed");
        } catch (Exception e) {
            errorCase = true;
            logger.error("Exception occured during putModelAndProcessAction", e);
        }
        if (errorCase) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(retrievedModel).build();
        }
        return Response.status(Response.Status.OK).entity(retrievedModel).build();
    }

    /**
     * REST service that accepts events for a model.
     *
     * @param test
     * @param dcaeEvent
     */
    @POST
    @Path("/dcae/event")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String postDcaeEvent(@QueryParam("test") String test, DcaeEvent dcaeEvent) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: Post dcae event", getPrincipalName());
        String userid = null;
        // TODO: allow auth checking to be turned off by removing the permission
        // type property
        if (cldsPermissionTypeClEvent != null && cldsPermissionTypeClEvent.length() > 0) {
            SecureServicePermission permissionEvent = SecureServicePermission.create(cldsPermissionTypeClEvent,
                    cldsPermissionInstance, dcaeEvent.getEvent());
            isAuthorized(permissionEvent);
            userid = getUserId();
        }
        // Flag indicates whether it is triggered by Validation Test button from
        // UI
        boolean isTest = false;
        if (test != null && test.equalsIgnoreCase("true")) {
            isTest = true;
        }
        int instanceCount = 0;
        if (dcaeEvent.getInstances() != null) {
            instanceCount = dcaeEvent.getInstances().size();
        }
        String msgInfo = "event=" + dcaeEvent.getEvent() + " serviceUUID=" + dcaeEvent.getServiceUUID()
                + " resourceUUID=" + dcaeEvent.getResourceUUID() + " artifactName=" + dcaeEvent.getArtifactName()
                + " instance count=" + instanceCount + " isTest=" + isTest;
        logger.info("POST dcae event {}", msgInfo);
        if (isTest) {
            logger.warn("Ignorning test event from DCAE");
        } else {
            if (DcaeEvent.EVENT_DEPLOYMENT.equalsIgnoreCase(dcaeEvent.getEvent())) {
                CldsModel.insertModelInstance(cldsDao, dcaeEvent, userid);
            } else {
                CldsEvent.insEvent(cldsDao, dcaeEvent.getControlName(), userid, dcaeEvent.getCldsActionCd(),
                        CldsEvent.ACTION_STATE_RECEIVED, null);
            }
        }
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Post dcae event success", this.getClass().getName());
        auditLogger.info("Post dcae event completed");
        return msgInfo;
    }

    /**
     * REST service that retrieves sdc services
     *
     * @throws GeneralSecurityException
     *             In case of issue when decryting the SDC password
     * @throws DecoderException
     *             In case of issues with the decoding of the Hex String
     */
    @GET
    @Path("/sdc/services")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSdcServices() throws GeneralSecurityException, DecoderException {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: GET sdc services", getPrincipalName());
        String retStr;
        try {
            retStr = createUiServiceFormatJson(sdcCatalogServices.getSdcServicesInformation(null));
        } catch (IOException e) {
            logger.error("IOException during SDC communication", e);
            throw new SdcCommunicationException("IOException during SDC communication", e);
        }
        logger.info("value of sdcServices : {}", retStr);
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get sdc services success", this.getClass().getName());
        auditLogger.info("GET sdc services completed");
        return retStr;
    }

    /**
     * REST service that retrieves total properties required by UI
     *
     * @throws IOException
     *             In case of issues
     */
    @GET
    @Path("/properties")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSdcProperties() throws IOException {
        return createPropertiesObjectByUUID("{}");
    }

    /**
     * REST service that retrieves total properties by using invariantUUID based
     * on refresh and non refresh
     *
     * @throws GeneralSecurityException
     *             In case of issues with the decryting the encrypted password
     * @throws DecoderException
     *             In case of issues with the decoding of the Hex String
     * @throws IOException
     *             In case of issue to convert CldsServiceCache to InputStream
     */
    @GET
    @Path("/properties/{serviceInvariantUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSdcPropertiesByServiceUUIDForRefresh(
            @PathParam("serviceInvariantUUID") String serviceInvariantUUID,
            @DefaultValue("false") @QueryParam("refresh") boolean refresh)
            throws GeneralSecurityException, DecoderException, IOException {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: GET sdc properties by uuid", getPrincipalName());
        CldsServiceData cldsServiceData = new CldsServiceData();
        cldsServiceData.setServiceInvariantUUID(serviceInvariantUUID);
        if (!refresh) {
            cldsServiceData = cldsDao.getCldsServiceCache(serviceInvariantUUID);
        }
        if (sdcCatalogServices.isCldsSdcCacheDataExpired(cldsServiceData)) {
            cldsServiceData = sdcCatalogServices.getCldsServiceDataWithAlarmConditions(serviceInvariantUUID);
            cldsDao.setCldsServiceCache(new CldsDbServiceCache(cldsServiceData));
        }
        // filter out VFs the user is not authorized for
        cldsServiceData.filterVfs(this);
        // format retrieved data into properties json
        String sdcProperties = sdcCatalogServices.createPropertiesObjectByUUID(cldsServiceData);
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get sdc properties by uuid success", this.getClass().getName());
        auditLogger.info("GET sdc properties by uuid completed");
        return sdcProperties;
    }

    /**
     * Determine if the user is authorized for a particular VF by its invariant
     * UUID.
     *
     * @param vfInvariantUuid
     * @throws NotAuthorizedException
     * @return
     */
    public boolean isAuthorizedForVf(String vfInvariantUuid) {
        if (cldsPermissionTypeFilterVf != null && !cldsPermissionTypeFilterVf.isEmpty()) {
            SecureServicePermission permission = SecureServicePermission.create(cldsPermissionTypeFilterVf,
                    cldsPermissionInstance, vfInvariantUuid);
            return isAuthorized(permission);
        } else {
            // if CLDS_PERMISSION_TYPE_FILTER_VF property is not provided, then
            // VF filtering is turned off
            logger.warn("VF filtering turned off");
            return true;
        }
    }

    /**
     * Determine if the user is authorized for a particular VF by its invariant
     * UUID. If not authorized, then NotAuthorizedException is thrown.
     *
     * @param model
     * @return
     */
    private boolean isAuthorizedForVf(CldsModel model) {
        String vf = ModelProperties.getVf(model);
        if (vf == null || vf.length() == 0) {
            logger.info("VF not found in model");
            return true;
        } else {
            return isAuthorizedForVf(vf);
        }
    }

    private String createUiServiceFormatJson(String responseStr) throws IOException {
        if (StringUtils.isBlank(responseStr)) {
            return "";
        }
        ObjectMapper objectMapper = JacksonUtils.getObjectMapperInstance();
        List<SdcServiceInfo> rawList = objectMapper.readValue(responseStr,
                objectMapper.getTypeFactory().constructCollectionType(List.class, SdcServiceInfo.class));
        ObjectNode invariantIdServiceNode = objectMapper.createObjectNode();
        ObjectNode serviceNode = objectMapper.createObjectNode();
        logger.info("value of cldsserviceiNfolist: {}", rawList);
        if (rawList != null && !rawList.isEmpty()) {
            List<SdcServiceInfo> cldsSdcServiceInfoList = sdcCatalogServices.removeDuplicateServices(rawList);
            for (SdcServiceInfo currCldsSdcServiceInfo : cldsSdcServiceInfoList) {
                if (currCldsSdcServiceInfo != null) {
                    invariantIdServiceNode.put(currCldsSdcServiceInfo.getInvariantUUID(),
                            currCldsSdcServiceInfo.getName());
                }
            }
            serviceNode.putPOJO("service", invariantIdServiceNode);
        }
        return serviceNode.toString();
    }

    private String createPropertiesObjectByUUID(String cldsResponseStr) throws IOException {
        ObjectMapper mapper = JacksonUtils.getObjectMapperInstance();
        SdcServiceDetail cldsSdcServiceDetail = mapper.readValue(cldsResponseStr, SdcServiceDetail.class);
        ObjectNode globalPropsJson = (ObjectNode) refProp.getJsonTemplate(GLOBAL_PROPERTIES_KEY);
        if (cldsSdcServiceDetail != null && cldsSdcServiceDetail.getUuid() != null) {
            /**
             * to create json with vf, alarm and locations
             */
            ObjectNode serviceObjectNode = createEmptyVfAlarmObject();
            ObjectNode vfObjectNode = mapper.createObjectNode();
            /**
             * to create json with vf and vfresourceId
             */
            createVfObjectNode(vfObjectNode, cldsSdcServiceDetail.getResources());
            serviceObjectNode.putPOJO(cldsSdcServiceDetail.getInvariantUUID(), vfObjectNode);
            ObjectNode byServiceBasicObjetNode = mapper.createObjectNode();
            byServiceBasicObjetNode.putPOJO("byService", serviceObjectNode);
            /**
             * to create json with VFC Node
             */
            ObjectNode emptyvfcobjectNode = createByVFCObjectNode(cldsSdcServiceDetail.getResources());
            byServiceBasicObjetNode.putPOJO("byVf", emptyvfcobjectNode);
            globalPropsJson.putPOJO("shared", byServiceBasicObjetNode);
            logger.info("valuie of objNode: {}", globalPropsJson);
        }
        return globalPropsJson.toString();
    }

    private ObjectNode createEmptyVfAlarmObject() {
        ObjectMapper mapper = JacksonUtils.getObjectMapperInstance();
        ObjectNode emptyObjectNode = mapper.createObjectNode();
        emptyObjectNode.put("", "");
        ObjectNode vfObjectNode = mapper.createObjectNode();
        vfObjectNode.putPOJO("vf", emptyObjectNode);
        vfObjectNode.putPOJO("location", emptyObjectNode);
        vfObjectNode.putPOJO("alarmCondition", emptyObjectNode);
        ObjectNode emptyServiceObjectNode = mapper.createObjectNode();
        emptyServiceObjectNode.putPOJO("", vfObjectNode);
        return emptyServiceObjectNode;
    }

    private void createVfObjectNode(ObjectNode vfObjectNode2, List<SdcResource> rawCldsSdcResourceList) {
        ObjectMapper mapper = JacksonUtils.getObjectMapperInstance();
        ObjectNode vfNode = mapper.createObjectNode();
        vfNode.put("", "");
        // To remove repeated resource instance name from
        // resourceInstanceList
        List<SdcResource> cldsSdcResourceList = sdcCatalogServices
                .removeDuplicateSdcResourceInstances(rawCldsSdcResourceList);
        /**
         * Creating vf resource node using cldsSdcResource Object
         */
        if (cldsSdcResourceList != null && !cldsSdcResourceList.isEmpty()) {
            for (SdcResource cldsSdcResource : cldsSdcResourceList) {
                if (cldsSdcResource != null && "VF".equalsIgnoreCase(cldsSdcResource.getResoucreType())) {
                    vfNode.put(cldsSdcResource.getResourceUUID(), cldsSdcResource.getResourceName());
                }
            }
        }
        vfObjectNode2.putPOJO("vf", vfNode);
        /**
         * creating location json object using properties file value
         */
        ObjectNode locationJsonNode;
        try {
            locationJsonNode = (ObjectNode) mapper.readValue(refProp.getStringValue("ui.location.default"),
                    JsonNode.class);
        } catch (IOException e) {
            logger.error("Unable to load ui.location.default JSON in clds-references.properties properly", e);
            throw new CldsConfigException(
                    "Unable to load ui.location.default JSON in clds-references.properties properly", e);
        }
        vfObjectNode2.putPOJO("location", locationJsonNode);
        /**
         * creating alarm json object using properties file value
         */
        String alarmStringValue = refProp.getStringValue("ui.alarm.default");
        logger.info("value of alarm: {}", alarmStringValue);
        ObjectNode alarmStringJsonNode;
        try {
            alarmStringJsonNode = (ObjectNode) mapper.readValue(alarmStringValue, JsonNode.class);
        } catch (IOException e) {
            logger.error("Unable to ui.alarm.default JSON in clds-references.properties properly", e);
            throw new CldsConfigException("Unable to load ui.alarm.default JSON in clds-references.properties properly",
                    e);
        }
        vfObjectNode2.putPOJO("alarmCondition", alarmStringJsonNode);
    }

    private ObjectNode createByVFCObjectNode(List<SdcResource> cldsSdcResourceList) {
        ObjectMapper mapper = JacksonUtils.getObjectMapperInstance();
        ObjectNode emptyObjectNode = mapper.createObjectNode();
        ObjectNode emptyvfcobjectNode = mapper.createObjectNode();
        ObjectNode vfCObjectNode = mapper.createObjectNode();
        vfCObjectNode.putPOJO("vfC", emptyObjectNode);
        ObjectNode subVfCObjectNode = mapper.createObjectNode();
        subVfCObjectNode.putPOJO("vfc", emptyObjectNode);
        if (cldsSdcResourceList != null && !cldsSdcResourceList.isEmpty()) {
            for (SdcResource cldsSdcResource : cldsSdcResourceList) {
                if (cldsSdcResource != null && "VF".equalsIgnoreCase(cldsSdcResource.getResoucreType())) {
                    vfCObjectNode.putPOJO(cldsSdcResource.getResourceUUID(), subVfCObjectNode);
                }
            }
        }
        emptyvfcobjectNode.putPOJO("", vfCObjectNode);
        return emptyvfcobjectNode;
    }

    @PUT
    @Path("/deploy/{modelName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deployModel(@PathParam("modelName") String modelName, CldsModel model) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: Deploy model", getPrincipalName());
        Boolean errorCase = false;
        try {
            fillInCldsModel(model);
            String bpmnJson = cldsBpmnTransformer.doXslTransformToString(model.getBpmnText());
            logger.info("PUT bpmnJson={}", bpmnJson);
            ModelProperties modelProp = new ModelProperties(modelName, model.getControlName(), CldsEvent.ACTION_DEPLOY,
                    false, bpmnJson, model.getPropText());
            checkForDuplicateServiceVf(modelName, model.getPropText());
            String deploymentId = "";
            // If model is already deployed then pass same deployment id
            if (model.getDeploymentId() != null && !model.getDeploymentId().isEmpty()) {
                deploymentId = model.getDeploymentId();
            } else {
                deploymentId = "closedLoop_" + UUID.randomUUID() + "_deploymentId";
            }
            String createNewDeploymentStatusUrl = dcaeDispatcherServices.createNewDeployment(deploymentId,
                    model.getTypeId(), modelProp.getGlobal().getDeployParameters());
            String operationStatus = dcaeDispatcherServices.getOperationStatusWithRetry(createNewDeploymentStatusUrl);
            if ("succeeded".equalsIgnoreCase(operationStatus)) {
                String artifactName = model.getControlName();
                if (artifactName != null) {
                    artifactName = artifactName + ".yml";
                }
                DcaeEvent dcaeEvent = new DcaeEvent();
                /* set dcae events */
                dcaeEvent.setArtifactName(artifactName);
                dcaeEvent.setEvent(DcaeEvent.EVENT_DEPLOYMENT);
                CldsEvent.insEvent(cldsDao, dcaeEvent.getControlName(), getUserId(), dcaeEvent.getCldsActionCd(),
                        CldsEvent.ACTION_STATE_RECEIVED, null);
                model.setDeploymentId(deploymentId);
                model.save(cldsDao, getUserId());
            } else {
                logger.info("Deploy model (" + modelName + ") failed...Operation Status is - " + operationStatus);
                throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Deploy model (" + modelName + ") failed...Operation Status is - " + operationStatus);
            }
            logger.info("Deploy model (" + modelName + ") succeeded...Deployment Id is - " + deploymentId);
            // audit log
            LoggingUtils.setTimeContext(startTime, new Date());
            LoggingUtils.setResponseContext("0", "Deploy model success", this.getClass().getName());
            auditLogger.info("Deploy model completed");
        } catch (Exception e) {
            errorCase = true;
            logger.error("Exception occured during deployModel", e);
        }
        if (errorCase) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(model).build();
        }
        return Response.status(Response.Status.OK).entity(model).build();
    }

    @PUT
    @Path("/undeploy/{modelName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unDeployModel(@PathParam("modelName") String modelName, CldsModel model) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: Undeploy model", getPrincipalName());
        Boolean errorCase = false;
        try {
            String operationStatusUndeployUrl = dcaeDispatcherServices.deleteExistingDeployment(model.getDeploymentId(),
                    model.getTypeId());
            String operationStatus = dcaeDispatcherServices.getOperationStatusWithRetry(operationStatusUndeployUrl);
            if ("succeeded".equalsIgnoreCase(operationStatus)) {
                String artifactName = model.getControlName();
                if (artifactName != null) {
                    artifactName = artifactName + ".yml";
                }
                DcaeEvent dcaeEvent = new DcaeEvent();
                // set dcae events
                dcaeEvent.setArtifactName(artifactName);
                dcaeEvent.setEvent(DcaeEvent.EVENT_UNDEPLOYMENT);
                CldsEvent.insEvent(cldsDao, model.getControlName(), getUserId(), dcaeEvent.getCldsActionCd(),
                        CldsEvent.ACTION_STATE_RECEIVED, null);
                model.setDeploymentId(null);
                model.save(cldsDao, getUserId());
            } else {
                logger.info("Undeploy model (" + modelName + ") failed...Operation Status is - " + operationStatus);
                throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Undeploy model (" + modelName + ") failed...Operation Status is - " + operationStatus);
            }
            logger.info("Undeploy model (" + modelName + ") succeeded.");
            // audit log
            LoggingUtils.setTimeContext(startTime, new Date());
            LoggingUtils.setResponseContext("0", "Undeploy model success", this.getClass().getName());
            auditLogger.info("Undeploy model completed");
        } catch (Exception e) {
            errorCase = true;
            logger.error("Exception occured during unDeployModel", e);
        }
        if (errorCase) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(model).build();
        }
        return Response.status(Response.Status.OK).entity(model).build();
    }

    private void checkForDuplicateServiceVf(String modelName, String modelPropText) throws IOException {
        JsonNode globalNode = JacksonUtils.getObjectMapperInstance().readTree(modelPropText).get("global");
        String service = AbstractModelElement.getValueByName(globalNode, "service");
        List<String> resourceVf = AbstractModelElement.getValuesByName(globalNode, "vf");
        if (service != null && resourceVf != null && !resourceVf.isEmpty()) {
            List<CldsModelProp> cldsModelPropList = cldsDao.getDeployedModelProperties();
            for (CldsModelProp cldsModelProp : cldsModelPropList) {
                JsonNode currentNode = JacksonUtils.getObjectMapperInstance().readTree(cldsModelProp.getPropText())
                        .get("global");
                String currentService = AbstractModelElement.getValueByName(currentNode, "service");
                List<String> currentVf = AbstractModelElement.getValuesByName(currentNode, "vf");
                if (currentVf != null && !currentVf.isEmpty()) {
                    if (!modelName.equalsIgnoreCase(cldsModelProp.getName()) && service.equalsIgnoreCase(currentService)
                            && resourceVf.get(0).equalsIgnoreCase(currentVf.get(0))) {
                        throw new BadRequestException("Same Service/VF already exists in " + cldsModelProp.getName()
                                + " model, please select different Service/VF.");
                    }
                }
            }
        }
    }
}