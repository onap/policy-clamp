/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.service;

import com.att.ajsc.common.AjscService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
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
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.json.simple.parser.ParseException;
import org.onap.clamp.clds.client.DcaeDispatcherServices;
import org.onap.clamp.clds.client.DcaeInventoryServices;
import org.onap.clamp.clds.client.SdcCatalogServices;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.exception.CldsConfigException;
import org.onap.clamp.clds.exception.SdcCommunicationException;
import org.onap.clamp.clds.model.CldsDBServiceCache;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsHealthCheck;
import org.onap.clamp.clds.model.CldsInfo;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.CldsSdcResource;
import org.onap.clamp.clds.model.CldsSdcServiceDetail;
import org.onap.clamp.clds.model.CldsSdcServiceInfo;
import org.onap.clamp.clds.model.CldsServiceData;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.model.DcaeEvent;
import org.onap.clamp.clds.model.ValueItem;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.clamp.clds.transform.XslTransformer;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Service to save and retrieve the CLDS model attributes.
 */
@AjscService
@Api(value = "/clds")
@Path("/clds")
public class CldsService extends SecureServiceBase {

    @Autowired
    private ApplicationContext      appContext;

    private static final String     RESOURCE_NAME = "clds-version.properties";

    @Value("${CLDS_PERMISSION_TYPE_CL:permission-type-cl}")
    private String                  cldsPersmissionTypeCl;

    @Value("${CLDS_PERMISSION_TYPE_CL_MANAGE:permission-type-cl-manage}")
    private String                  cldsPermissionTypeClManage;

    @Value("${CLDS_PERMISSION_TYPE_CL_EVENT:permission-type-cl-event}")
    private String                  cldsPermissionTypeClEvent;

    @Value("${CLDS_PERMISSION_TYPE_FILTER_VF:permission-type-filter-vf}")
    private String                  cldsPermissionTypeFilterVf;

    @Value("${CLDS_PERMISSION_TYPE_TEMPLATE:permission-type-template}")
    private String                  cldsPermissionTypeTemplate;

    @Value("${CLDS_PERMISSION_INSTANCE:dev}")
    private String                  cldsPermissionInstance;

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

    @Value("${org.onap.clamp.config.files.globalClds:'classpath:/clds/globalClds.properties'}")
    private String                 globalClds;

    private Properties             globalCldsProperties;

    @Autowired
    private CldsDao                cldsDao;
    @Autowired
    private RuntimeService         runtimeService;
    @Autowired
    private XslTransformer         cldsBpmnTransformer;

    @Autowired
    private RefProp                refProp;

    @Autowired
    private SdcCatalogServices     sdcCatalogServices;

    @Autowired
    private DcaeDispatcherServices dcaeDispatcherServices;

    @Autowired
    private DcaeInventoryServices  dcaeInventoryServices;

    public CldsService() {
    }

    public CldsService(RefProp refProp) {
        this.refProp = refProp;
    }

    /*
     *
     * CLDS IFO service will return 3 things 1. User Name 2. CLDS code version
     * that is currently installed from pom.xml file 3. User permissions
     *
     */

    @GET
    @Path("/cldsInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public CldsInfo getCldsInfo() {

        CldsInfo cldsInfo = new CldsInfo();

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
        return cldsInfo;
    }

    @GET
    @Path("/healthcheck")
    @Produces(MediaType.APPLICATION_JSON)
    public CldsHealthCheck gethealthcheck() {

        CldsHealthCheck cldsHealthCheck = new CldsHealthCheck();

        try {
            cldsDao.doHealthCheck();
            cldsHealthCheck.setHealthCheckComponent("CLDS-APP");
            cldsHealthCheck.setHealthCheckStatus("UP");
            cldsHealthCheck.setDescription("OK");
        } catch (Exception e) {
            logger.error("CLAMP application DB Error", e);
            cldsHealthCheck.setHealthCheckComponent("CLDS-APP");
            cldsHealthCheck.setHealthCheckStatus("DOWN");
            cldsHealthCheck.setDescription("NOT-OK");
        }
        return cldsHealthCheck;

    }

    /**
     * REST service that retrieves BPMN for a CLDS model name from the database.
     * This is subset of the json getModel. This is only expected to be used for
     * testing purposes, not by the UI.
     *
     * @param modelName
     * @return bpmn xml text - content of bpmn given name
     */
    @ApiOperation(value = "Retrieves BPMN for a CLDS model name from the database", notes = "This is only expected to be used for testing purposes, not by the UI", response = String.class)
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
     * REST service that saves BPMN for a CLDS model by name in the database.
     * This is subset of the json putModel. This is only expected to be used for
     * testing purposes, not by the UI.
     *
     * @param modelName
     */
    @ApiOperation(value = "Saves BPMN for a CLDS model by name in the database", notes = "This is only expected to be used for testing purposes, not by the UI", response = String.class)
    @PUT
    @Path("/model/bpmn/{modelName}")
    @Consumes(MediaType.TEXT_XML)
    public String putBpmnXml(@PathParam("modelName") String modelName, String bpmnText) {
        LoggingUtils.setRequestContext("CldsService: PUT model bpmn", getPrincipalName());
        isAuthorized(permissionUpdateCl);
        logger.info("PUT bpmnText for modelName={}", modelName);
        logger.info("PUT bpmnText={}", bpmnText);
        CldsModel cldsModel = CldsModel.retrieve(cldsDao, modelName, true);
        cldsModel.setBpmnText(bpmnText);
        cldsModel.save(cldsDao, getUserId());
        // audit log
        LoggingUtils.setTimeContext(new Date(), new Date());
        LoggingUtils.setResponseContext("0", "Put model bpmn success", this.getClass().getName());
        auditLogger.info("PUT model bpmn completed");
        return "wrote bpmnText for modelName=" + modelName;
    }

    /**
     * REST service that retrieves image for a CLDS model name from the
     * database. This is subset of the json getModel. This is only expected to
     * be used for testing purposes, not by the UI.
     *
     * @param modelName
     * @return image xml text - content of image given name
     */
    @ApiOperation(value = "Retrieves image for a CLDS model name from the database", notes = "This is only expected to be used for testing purposes, not by the UI", response = String.class)
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
     * REST service that saves image for a CLDS model by name in the database.
     * This is subset of the json putModel. This is only expected to be used for
     * testing purposes, not by the UI.
     *
     * @param modelName
     */
    @ApiOperation(value = "Saves image for a CLDS model by name in the database", notes = "This is only expected to be used for testing purposes, not by the UI", response = String.class)
    @PUT
    @Path("/model/image/{modelName}")
    @Consumes(MediaType.TEXT_XML)
    public String putImageXml(@PathParam("modelName") String modelName, String imageText) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: PUT model image", getPrincipalName());
        isAuthorized(permissionUpdateCl);
        logger.info("PUT iamgeText for modelName={}", modelName);
        logger.info("PUT imageText={}", imageText);
        CldsModel cldsModel = CldsModel.retrieve(cldsDao, modelName, true);
        cldsModel.setImageText(imageText);
        cldsModel.save(cldsDao, getUserId());
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Put model image success", this.getClass().getName());
        auditLogger.info("PUT model image completed");
        return "wrote imageText for modelName=" + modelName;
    }

    /**
     * REST service that retrieves a CLDS model by name from the database.
     *
     * @param modelName
     * @return clds model - clds model for the given model name
     */
    @ApiOperation(value = "Retrieves a CLDS model by name from the database", notes = "", response = String.class)
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
        cldsModel.setUserAuthorizedToUpdate(isAuthorizedNoException(permissionUpdateCl));

        /**
         * Checking condition whether our CLDS model can call INventory Method
         */
        if (cldsModel.canInventoryCall()) {
            try {
                /*
                 * Below is the method to for inventory call and DB insert for
                 * event methods
                 */
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
    @ApiOperation(value = "Saves a CLDS model by name in the database", notes = "", response = String.class)
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
                cldsModel.setDocId(template.getPropId());
            }
        }
        cldsModel.save(cldsDao, getUserId());
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
    @ApiOperation(value = "Retrieves a list of CLDS model names", notes = "", response = String.class)
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

    /**
     * REST service that saves and processes an action for a CLDS model by name.
     *
     * @param action
     * @param modelName
     * @param test
     * @param model
     * @return
     * @throws TransformerException
     * @throws ParseException
     */
    @ApiOperation(value = "Saves and processes an action for a CLDS model by name", notes = "", response = String.class)
    @PUT
    @Path("/action/{action}/{modelName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CldsModel putModelAndProcessAction(@PathParam("action") String action,
            @PathParam("modelName") String modelName, @QueryParam("test") String test, CldsModel model)
            throws TransformerException, ParseException {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: Process model action", getPrincipalName());
        String actionCd = action.toUpperCase();
        SecureServicePermission permisionManage = SecureServicePermission.create(cldsPermissionTypeClManage,
                cldsPermissionInstance, actionCd);
        isAuthorized(permisionManage);
        isAuthorizedForVf(model);
        String userid = getUserId();
        String actionStateCd = CldsEvent.ACTION_STATE_INITIATED;
        String processDefinitionKey = "clds-process-action-wf";

        logger.info("PUT actionCd={}", actionCd);
        logger.info("PUT actionStateCd={}", actionStateCd);
        logger.info("PUT processDefinitionKey={}", processDefinitionKey);
        logger.info("PUT modelName={}", modelName);
        logger.info("PUT test={}", test);
        logger.info("PUT bpmnText={}", model.getBpmnText());
        logger.info("PUT propText={}", model.getPropText());
        logger.info("PUT userid={}", userid);
        logger.info("PUT getTypeId={}", model.getTypeId());
        logger.info("PUT deploymentId={}", model.getDeploymentId());

        if (model.getTemplateName() != null) {
            CldsTemplate template = cldsDao.getTemplate(model.getTemplateName());
            if (template != null) {
                model.setTemplateId(template.getId());
                model.setDocText(template.getPropText());
                model.setDocId(template.getPropId());
            }
        }
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

        // Flag indicates whether it is triggered by Validation Test button from
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

        // input variables to camunda process
        Map<String, Object> variables = new HashMap<>();
        variables.put("actionCd", actionCd);
        variables.put("modelProp", prop.getBytes());
        variables.put("modelBpmnProp", bpmnJson);
        variables.put("modelName", modelName);
        variables.put("controlName", controlName);
        variables.put("docText", docText.getBytes());
        variables.put("isTest", isTest);
        variables.put("userid", userid);
        variables.put("isInsertTestEvent", isInsertTestEvent);
        logger.info("modelProp - " + prop);
        logger.info("docText - " + docText);

        // start camunda process
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);

        // log process info
        logger.info("Started processDefinitionId={}, processInstanceId={}", pi.getProcessDefinitionId(),
                pi.getProcessInstanceId());

        // refresh model info from db (get fresh event info)
        CldsModel retreivedModel = CldsModel.retrieve(cldsDao, modelName, false);

        if (actionCd.equalsIgnoreCase(CldsEvent.ACTION_SUBMIT)
                || actionCd.equalsIgnoreCase(CldsEvent.ACTION_RESUBMIT)) {
            // To verify inventory status and modify model status to distribute
            dcaeInventoryServices.setEventInventory(retreivedModel, getUserId());
            retreivedModel.save(cldsDao, getUserId());
        }
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Process model action success", this.getClass().getName());
        auditLogger.info("Process model action completed");

        return retreivedModel;
    }

    /**
     * REST service that accepts events for a model.
     *
     * @param test
     * @param dcaeEvent
     */
    @ApiOperation(value = "Accepts events for a model", notes = "", response = String.class)
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
     * @throws Exception
     */
    @ApiOperation(value = "Retrieves sdc services", notes = "", response = String.class)
    @GET
    @Path("/sdc/services")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSdcServices() {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: GET sdc services", getPrincipalName());
        String retStr;

        String responseStr = sdcCatalogServices.getSdcServicesInformation(null);
        try {
            retStr = createUiServiceFormatJson(responseStr);
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
     *
     */
    @ApiOperation(value = "Retrieves total properties required by UI", notes = "", response = String.class)
    @GET
    @Path("/properties")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSdcProperties() throws IOException {
        return createPropertiesObjectByUUID(getGlobalCldsString(), "{}");
    }

    /**
     * REST service that retrieves total properties by using invariantUUID based
     * on refresh and non refresh
     * 
     */
    @ApiOperation(value = "Retrieves total properties by using invariantUUID based on refresh and non refresh", notes = "", response = String.class)
    @GET
    @Path("/properties/{serviceInvariantUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSdcPropertiesByServiceUUIDForRefresh(
            @PathParam("serviceInvariantUUID") String serviceInvariantUUID,
            @DefaultValue("false") @QueryParam("refresh") String refresh) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: GET sdc properties by uuid", getPrincipalName());
        CldsServiceData cldsServiceData = new CldsServiceData();
        cldsServiceData.setServiceInvariantUUID(serviceInvariantUUID);

        boolean isCldsSdcDataExpired = true;
        // To getcldsService information from database cache using invariantUUID
        // only when refresh = false
        if (refresh != null && refresh.equalsIgnoreCase("false")) {
            cldsServiceData = cldsServiceData.getCldsServiceCache(cldsDao, serviceInvariantUUID);
            // If cldsService is available in database Cache , verify is data
            // expired or not
            if (cldsServiceData != null) {
                isCldsSdcDataExpired = sdcCatalogServices.isCldsSdcCacheDataExpired(cldsServiceData);
            }
        }
        // If user Requested for refresh or database cache expired , get all
        // data from sdc api.
        if ((refresh != null && refresh.equalsIgnoreCase("true")) || isCldsSdcDataExpired) {
            cldsServiceData = sdcCatalogServices.getCldsServiceDataWithAlarmConditions(serviceInvariantUUID);
            CldsDBServiceCache cldsDBServiceCache = sdcCatalogServices
                    .getCldsDbServiceCacheUsingCldsServiceData(cldsServiceData);
            if (cldsDBServiceCache != null && cldsDBServiceCache.getInvariantId() != null
                    && cldsDBServiceCache.getServiceId() != null) {
                cldsServiceData.setCldsServiceCache(cldsDao, cldsDBServiceCache);
            }
        }

        // filter out VFs the user is not authorized for
        cldsServiceData.filterVfs(this);

        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get sdc properties by uuid success", this.getClass().getName());
        auditLogger.info("GET sdc properties by uuid completed");

        // format retrieved data into properties json
        return sdcCatalogServices.createPropertiesObjectByUUID(getGlobalCldsString(), cldsServiceData);
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
        ObjectMapper objectMapper = new ObjectMapper();
        List<CldsSdcServiceInfo> rawList = objectMapper.readValue(responseStr,
                objectMapper.getTypeFactory().constructCollectionType(List.class, CldsSdcServiceInfo.class));
        ObjectNode invariantIdServiceNode = objectMapper.createObjectNode();
        ObjectNode serviceNode = objectMapper.createObjectNode();
        logger.info("value of cldsserviceiNfolist: {}", rawList);
        if (rawList != null && !rawList.isEmpty()) {
            List<CldsSdcServiceInfo> cldsSdcServiceInfoList = sdcCatalogServices.removeDuplicateServices(rawList);

            for (CldsSdcServiceInfo currCldsSdcServiceInfo : cldsSdcServiceInfoList) {
                if (currCldsSdcServiceInfo != null) {
                    invariantIdServiceNode.put(currCldsSdcServiceInfo.getInvariantUUID(),
                            currCldsSdcServiceInfo.getName());
                }
            }
            serviceNode.putPOJO("service", invariantIdServiceNode);
        }
        return serviceNode.toString();
    }

    private String createPropertiesObjectByUUID(String globalProps, String cldsResponseStr) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        CldsSdcServiceDetail cldsSdcServiceDetail = mapper.readValue(cldsResponseStr, CldsSdcServiceDetail.class);
        ObjectNode globalPropsJson = null;
        if (cldsSdcServiceDetail != null && cldsSdcServiceDetail.getUuid() != null) {
            /**
             * to create json with vf, alarm and locations
             */
            ObjectNode serviceObjectNode = createEmptyVfAlarmObject(mapper);
            ObjectNode vfObjectNode = mapper.createObjectNode();

            /**
             * to create json with vf and vfresourceId
             */
            createVfObjectNode(vfObjectNode, mapper, cldsSdcServiceDetail.getResources());
            serviceObjectNode.putPOJO(cldsSdcServiceDetail.getInvariantUUID(), vfObjectNode);
            ObjectNode byServiceBasicObjetNode = mapper.createObjectNode();
            byServiceBasicObjetNode.putPOJO("byService", serviceObjectNode);

            /**
             * to create json with VFC Node
             */
            ObjectNode emptyvfcobjectNode = createByVFCObjectNode(mapper, cldsSdcServiceDetail.getResources());
            byServiceBasicObjetNode.putPOJO("byVf", emptyvfcobjectNode);
            globalPropsJson = (ObjectNode) mapper.readValue(globalProps, JsonNode.class);
            globalPropsJson.putPOJO("shared", byServiceBasicObjetNode);
            logger.info("valuie of objNode: {}", globalPropsJson);
        } else {
            /**
             * to create json with total properties when no serviceUUID passed
             */
            globalPropsJson = (ObjectNode) mapper.readValue(globalProps, JsonNode.class);
        }
        return globalPropsJson.toString();
    }

    private ObjectNode createEmptyVfAlarmObject(ObjectMapper mapper) {
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

    private void createVfObjectNode(ObjectNode vfObjectNode2, ObjectMapper mapper,
            List<CldsSdcResource> rawCldsSdcResourceList) {
        ObjectNode vfNode = mapper.createObjectNode();
        vfNode.put("", "");

        // To remove repeated resource instance name from
        // resourceInstanceList
        List<CldsSdcResource> cldsSdcResourceList = sdcCatalogServices
                .removeDuplicateSdcResourceInstances(rawCldsSdcResourceList);
        /**
         * Creating vf resource node using cldsSdcResource Object
         */
        if (cldsSdcResourceList != null && !cldsSdcResourceList.isEmpty()) {
            for (CldsSdcResource cldsSdcResource : cldsSdcResourceList) {
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

    private ObjectNode createByVFCObjectNode(ObjectMapper mapper, List<CldsSdcResource> cldsSdcResourceList) {
        ObjectNode emptyObjectNode = mapper.createObjectNode();
        ObjectNode emptyvfcobjectNode = mapper.createObjectNode();
        ObjectNode vfCObjectNode = mapper.createObjectNode();
        vfCObjectNode.putPOJO("vfC", emptyObjectNode);
        ObjectNode subVfCObjectNode = mapper.createObjectNode();
        subVfCObjectNode.putPOJO("vfc", emptyObjectNode);
        if (cldsSdcResourceList != null && !cldsSdcResourceList.isEmpty()) {
            for (CldsSdcResource cldsSdcResource : cldsSdcResourceList) {
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
    public CldsModel deployModel(@PathParam("action") String action, @PathParam("modelName") String modelName,
            @QueryParam("test") String test, CldsModel model) throws IOException {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: Deploy model", getPrincipalName());
        String deploymentId = "closedLoop_" + UUID.randomUUID() + "_deploymentId";
        String createNewDeploymentStatusUrl = dcaeDispatcherServices.createNewDeployment(deploymentId,
                model.getTypeId());
        String operationStatus = "processing";
        long waitingTime = System.nanoTime() + TimeUnit.MINUTES.toNanos(10);
        while ("processing".equalsIgnoreCase(operationStatus)) {
            // Break the loop if waiting for more than 10 mins
            if (waitingTime < System.nanoTime()) {
                break;
            }
            operationStatus = dcaeDispatcherServices.getOperationStatus(createNewDeploymentStatusUrl);
        }
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
        return model;
    }

    @PUT
    @Path("/undeploy/{modelName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CldsModel unDeployModel(@PathParam("action") String action, @PathParam("modelName") String modelName,
            @QueryParam("test") String test, CldsModel model) throws IOException {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsService: Undeploy model", getPrincipalName());
        String operationStatusUndeployUrl = dcaeDispatcherServices.deleteExistingDeployment(model.getDeploymentId(),
                model.getTypeId());
        String operationStatus = "processing";
        long waitingTime = System.nanoTime() + TimeUnit.MINUTES.toNanos(10);
        while ("processing".equalsIgnoreCase(operationStatus)) {
            if (waitingTime < System.nanoTime()) {
                break;
            }
            operationStatus = dcaeDispatcherServices.getOperationStatus(operationStatusUndeployUrl);
        }
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
        return model;
    }

    private String getGlobalCldsString() {
        try {
            if (null == globalCldsProperties) {
                globalCldsProperties = new Properties();
                globalCldsProperties.load(appContext.getResource(globalClds).getInputStream());
            }
            return (String) globalCldsProperties.get("globalCldsProps");
        } catch (IOException e) {
            logger.error("Unable to load the globalClds due to an exception", e);
            throw new CldsConfigException("Unable to load the globalClds due to an exception", e);
        }
    }
}
