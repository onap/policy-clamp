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
 * Modifications copyright (c) 2018 Nokia
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
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
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
import org.onap.clamp.clds.util.ONAPLogConstants;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Service to save and retrieve the CLDS model attributes.
 */
@Component
public class CldsService extends SecureServiceBase {

    @Produce(uri = "direct:processSubmit")
    private CamelProxy camelProxy;
    protected static final EELFLogger securityLogger = EELFManager.getInstance().getSecurityLogger();
    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsService.class);

    public static final String GLOBAL_PROPERTIES_KEY = "files.globalProperties";
    private final String cldsPersmissionTypeCl;
    private final String cldsPermissionTypeClManage;
    private final String cldsPermissionTypeClEvent;
    private final String cldsPermissionTypeFilterVf;
    private final String cldsPermissionTypeTemplate;
    private final String cldsPermissionInstance;
    final SecureServicePermission permissionReadCl;
    final SecureServicePermission permissionUpdateCl;
    final SecureServicePermission permissionReadTemplate;
    final SecureServicePermission permissionUpdateTemplate;
    final SecureServicePermission permissionReadTosca;
    final SecureServicePermission permissionUpdateTosca;

    private final CldsDao cldsDao;
    private final XslTransformer cldsBpmnTransformer;
    private final ClampProperties refProp;
    private final SdcCatalogServices sdcCatalogServices;
    private final DcaeDispatcherServices dcaeDispatcherServices;
    private final DcaeInventoryServices dcaeInventoryServices;
    private LoggingUtils util = new LoggingUtils(logger);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    public CldsService(CldsDao cldsDao, XslTransformer cldsBpmnTransformer, ClampProperties refProp,
        SdcCatalogServices sdcCatalogServices, DcaeDispatcherServices dcaeDispatcherServices,
        DcaeInventoryServices dcaeInventoryServices,
        @Value("${clamp.config.security.permission.type.cl:permission-type-cl}") String cldsPersmissionTypeCl,
        @Value("${clamp.config.security.permission.type.cl.manage:permission-type-cl-manage}") String cldsPermissionTypeClManage,
        @Value("${clamp.config.security.permission.type.cl.event:permission-type-cl-event}") String cldsPermissionTypeClEvent,
        @Value("${clamp.config.security.permission.type.filter.vf:permission-type-filter-vf}") String cldsPermissionTypeFilterVf,
        @Value("${clamp.config.security.permission.type.template:permission-type-template}") String cldsPermissionTypeTemplate,
        @Value("${clamp.config.security.permission.type.tosca:permission-type-tosca}") String cldsPermissionTypeTosca,
        @Value("${clamp.config.security.permission.instance:dev}") String cldsPermissionInstance) {
        this.cldsDao = cldsDao;
        this.cldsBpmnTransformer = cldsBpmnTransformer;
        this.refProp = refProp;
        this.sdcCatalogServices = sdcCatalogServices;
        this.dcaeDispatcherServices = dcaeDispatcherServices;
        this.dcaeInventoryServices = dcaeInventoryServices;
        this.cldsPersmissionTypeCl = cldsPersmissionTypeCl;
        this.cldsPermissionTypeClManage = cldsPermissionTypeClManage;
        this.cldsPermissionTypeClEvent = cldsPermissionTypeClEvent;
        this.cldsPermissionTypeFilterVf = cldsPermissionTypeFilterVf;
        this.cldsPermissionTypeTemplate = cldsPermissionTypeTemplate;
        this.cldsPermissionInstance = cldsPermissionInstance;
        permissionReadCl = SecureServicePermission.create(cldsPersmissionTypeCl, cldsPermissionInstance, "read");
        permissionUpdateCl = SecureServicePermission.create(cldsPersmissionTypeCl, cldsPermissionInstance, "update");
        permissionReadTemplate = SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance,
            "read");
        permissionUpdateTemplate = SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance,
            "update");
        permissionReadTosca = SecureServicePermission.create(cldsPermissionTypeTosca, cldsPermissionInstance, "read");
        permissionUpdateTosca = SecureServicePermission.create(cldsPermissionTypeTosca, cldsPermissionInstance,
            "update");
    }

    /*
     * @return list of CLDS-Monitoring-Details: CLOSELOOP_NAME | Close loop name
     * used in the CLDS application (prefix: ClosedLoop- + unique ClosedLoop ID)
     * MODEL_NAME | Model Name in CLDS application SERVICE_TYPE_ID | TypeId returned
     * from the DCAE application when the ClosedLoop is submitted
     * (DCAEServiceTypeRequest generated in DCAE application). DEPLOYMENT_ID | Id
     * generated when the ClosedLoop is deployed in DCAE. TEMPLATE_NAME | Template
     * used to generate the ClosedLoop model. ACTION_CD | Current state of the
     * ClosedLoop in CLDS application.
     */
    public List<CldsMonitoringDetails> getCldsDetails() {
        util.entering(request, "CldsService: GET model details");
        Date startTime = new Date();
        List<CldsMonitoringDetails> cldsMonitoringDetailsList = cldsDao.getCLDSMonitoringDetails();
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("GET cldsDetails completed");
        util.exiting("200", "Get cldsDetails success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return cldsMonitoringDetailsList;
    }

    /*
     * CLDS IFO service will return 3 things 1. User Name 2. CLDS code version that
     * is currently installed from pom.xml file 3. User permissions
     */
    public CldsInfo getCldsInfo() {
        util.entering(request, "CldsService: GET cldsInfo");
        Date startTime = new Date();
        LoggingUtils.setTimeContext(startTime, new Date());

        CldsInfoProvider cldsInfoProvider = new CldsInfoProvider(this);
        CldsInfo cldsInfo = cldsInfoProvider.getCldsInfo();

        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        securityLogger.info("GET cldsInfo completed");
        util.exiting("200", "Get cldsInfo success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return cldsInfo;
    }

    /**
     * REST service that retrieves BPMN for a CLDS model name from the database.
     * This is subset of the json getModel. This is only expected to be used for
     * testing purposes, not by the UI.
     *
     * @param modelName
     * @return bpmn xml text - content of bpmn given name
     */
    public String getBpmnXml(String modelName) {
        util.entering(request, "CldsService: GET model bpmn");
        Date startTime = new Date();
        isAuthorized(permissionReadCl);
        logger.info("GET bpmnText for modelName={}", modelName);
        CldsModel model = CldsModel.retrieve(cldsDao, modelName, false);
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("GET model bpmn completed");
        util.exiting("200", "Get model bpmn success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return model.getBpmnText();
    }

    /**
     * REST service that retrieves image for a CLDS model name from the database.
     * This is subset of the json getModel. This is only expected to be used for
     * testing purposes, not by the UI.
     *
     * @param modelName
     * @return image xml text - content of image given name
     */
    public String getImageXml(String modelName) {
        util.entering(request, "CldsService: GET model image");
        Date startTime = new Date();
        isAuthorized(permissionReadCl);
        logger.info("GET imageText for modelName={}", modelName);
        CldsModel model = CldsModel.retrieve(cldsDao, modelName, false);
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("GET model image completed");
        util.exiting("200", "Get model image success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return model.getImageText();
    }

    /**
     * REST service that retrieves a CLDS model by name from the database.
     *
     * @param modelName
     * @return clds model - clds model for the given model name
     */
    public CldsModel getModel(String modelName) {
        util.entering(request, "CldsService: GET model");
        Date startTime = new Date();
        isAuthorized(permissionReadCl);
        logger.debug("GET model for  modelName={}", modelName);
        CldsModel cldsModel = CldsModel.retrieve(cldsDao, modelName, false);
        isAuthorizedForVf(cldsModel);
        // Try an update for DCAE status
        // Checking condition whether our CLDS model can call Inventory Method
        try {
            // Method to call dcae inventory and invoke insert event method
            if (cldsModel.canDcaeInventoryCall()
                && !cldsModel.getTemplateName().startsWith(CsarInstallerImpl.TEMPLATE_NAME_PREFIX)) {
                dcaeInventoryServices.setEventInventory(cldsModel, getUserId());
            }
            // This is a blocking call
            if (cldsModel.getEvent().isActionCd(CldsEvent.ACTION_DEPLOY)
                && !CldsModel.STATUS_ACTIVE.equals(cldsModel.getStatus()) && cldsModel.getDeploymentId() != null
                && cldsModel.getDeploymentStatusUrl() != null) {
                checkDcaeDeploymentStatus(cldsModel, CldsEvent.ACTION_DEPLOY, false);
                // Refresh the model object in any cases for new event
                cldsModel = CldsModel.retrieve(cldsDao, cldsModel.getName(), false);
            }
        } catch (Exception e) {
            LoggingUtils.setErrorContext("900", "Set event inventory error");
            logger.error("getModel set event Inventory error:" + e);
        }
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("GET model completed");
        util.exiting("200", "Get model success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return cldsModel;
    }

    /**
     * REST service that saves a CLDS model by name in the database.
     *
     * @param modelName
     */
    public CldsModel putModel(String modelName, CldsModel cldsModel) {
        util.entering(request, "CldsService: PUT model");
        Date startTime = new Date();
        isAuthorized(permissionUpdateCl);
        isAuthorizedForVf(cldsModel);
        logger.info("PUT model for  modelName={}", modelName);
        logger.info("PUT bpmnText={}", cldsModel.getBpmnText());
        logger.info("PUT propText={}", cldsModel.getPropText());
        logger.info("PUT imageText={}", cldsModel.getImageText());
        fillInCldsModel(cldsModel);
        cldsModel.save(cldsDao, getUserId());

        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("PUT model completed");
        util.exiting("200", "Put model success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return cldsModel;
    }

    /**
     * REST service that retrieves a list of CLDS model names.
     *
     * @return model names in JSON
     */
    public List<ValueItem> getModelNames() {
        util.entering(request, "CldsService: GET model names");
        Date startTime = new Date();
        isAuthorized(permissionReadCl);
        logger.info("GET list of model names");
        List<ValueItem> names = cldsDao.getModelNames();
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("GET model names completed");
        util.exiting("200", "Get model names success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
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
     * @param validateFlag
     * @param model
     * @return
     * @throws TransformerException
     *         In case of issues when doing the XSLT of the BPMN flow
     * @throws ParseException
     *         In case of issues when parsing the JSON
     * @throws GeneralSecurityException
     *         In case of issues when decrypting the password
     * @throws DecoderException
     *         In case of issues with the Hex String decoding
     */
    public ResponseEntity<?> putModelAndProcessAction(String action, String modelName, String test, CldsModel model)
        throws TransformerException, ParseException {
        util.entering(request, "CldsService: Process model action");
        Date startTime = new Date();
        String errorMessage = "";
        String actionCd = "";
        try {
            actionCd = action.toUpperCase();
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
            // save model to db just in case
            model.save(cldsDao, getUserId());

            // get vars and format if necessary
            String prop = model.getPropText();
            String bpmn = model.getBpmnText();
            String docText = model.getDocText();
            String controlName = model.getControlName();
            String bpmnJson = cldsBpmnTransformer.doXslTransformToString(bpmn);
            logger.info("PUT bpmnJson={}", bpmnJson);
            // Test flag coming from UI or from Clamp config
            boolean isTest = Boolean.parseBoolean(test)
                || Boolean.parseBoolean(refProp.getStringValue("action.test.override"));
            logger.info("PUT isTest={}", isTest);
            boolean isInsertTestEvent = Boolean.parseBoolean(refProp.getStringValue("action.insert.test.event"));
            logger.info("PUT isInsertTestEvent={}", isInsertTestEvent);
            // determine if requested action is permitted
            model.validateAction(actionCd);
            logger.info("modelProp - " + prop);
            logger.info("docText - " + docText);
            try {
                String result = camelProxy.executeAction(actionCd, prop, bpmnJson, modelName, controlName, docText,
                    isTest, userId, isInsertTestEvent, model.getEvent().getActionCd());
                logger.info("Starting Camel flow on request, result is: ", result);
            } catch (SdcCommunicationException | PolicyClientException | BadRequestException e) {
                logger.error("Exception occured during invoking Camel process", e);
                errorMessage = e.getMessage();
            }
            // audit log
            LoggingUtils.setTimeContext(startTime, new Date());
            auditLogger.info("Process model action completed");
        } catch (Exception e) {
            logger.error("Exception occured during putModelAndProcessAction", e);
            errorMessage = e.getMessage();
        }

        if (!errorMessage.isEmpty()) {
            CldsEvent.insEvent(cldsDao, model.getControlName(), getUserId(), actionCd, CldsEvent.ACTION_STATE_ERROR,
                null);
            // Need a refresh as new events have been inserted
            model = CldsModel.retrieve(cldsDao, modelName, false);
            model.setErrorMessageForUi(errorMessage);
            util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "putModelAndProcessAction failed", Level.INFO,
                ONAPLogConstants.ResponseStatus.ERROR);
            return new ResponseEntity<>(model, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            // Need a refresh as new events have been inserted, could have been deleted so
            // not blocking call
            model = CldsModel.retrieve(cldsDao, modelName, true);
            util.exiting(HttpStatus.OK.toString(), "Successful", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
            return new ResponseEntity<>(model, HttpStatus.OK);
        }
    }

    /**
     * REST service that accepts events for a model.
     *
     * @param test
     * @param dcaeEvent
     */
    public String postDcaeEvent(String test, DcaeEvent dcaeEvent) {
        util.entering(request, "CldsService: Post dcae event");
        Date startTime = new Date();
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
        boolean isTest = Boolean.parseBoolean(test);
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
        auditLogger.info("Post dcae event completed");
        util.exiting("200", "Post dcae event success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return msgInfo;
    }

    /**
     * REST service that retrieves sdc services
     *
     * @throws GeneralSecurityException
     *         In case of issue when decryting the SDC password
     * @throws DecoderException
     *         In case of issues with the decoding of the Hex String
     */
    public String getSdcServices() throws GeneralSecurityException, DecoderException {
        util.entering(request, "CldsService: GET sdc services");
        Date startTime = new Date();
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
        auditLogger.info("GET sdc services completed");
        util.exiting("200", "Get sdc services success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return retStr;
    }

    /**
     * REST service that retrieves total properties required by UI
     *
     * @throws IOException
     *         In case of issues
     */
    public String getSdcProperties() throws IOException {
        return createPropertiesObjectByUUID("{}");
    }

    /**
     * REST service that retrieves total properties by using invariantUUID based on
     * refresh and non refresh
     *
     * @throws GeneralSecurityException
     *         In case of issues with the decryting the encrypted password
     * @throws DecoderException
     *         In case of issues with the decoding of the Hex String
     * @throws IOException
     *         In case of issue to convert CldsServiceCache to InputStream
     */
    public String getSdcPropertiesByServiceUUIDForRefresh(String serviceInvariantUUID, Boolean refresh)
        throws GeneralSecurityException, DecoderException, IOException {
        util.entering(request, "CldsService: GET sdc properties by uuid");
        Date startTime = new Date();
        CldsServiceData cldsServiceData = new CldsServiceData();
        cldsServiceData.setServiceInvariantUUID(serviceInvariantUUID);
        if (!Optional.ofNullable(refresh).orElse(false)) {
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
        auditLogger.info("GET sdc properties by uuid completed");
        util.exiting("200", "Get sdc properties by uuid success", Level.INFO,
            ONAPLogConstants.ResponseStatus.COMPLETED);
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

    public ResponseEntity<CldsModel> deployModel(String modelName, CldsModel model) {
        util.entering(request, "CldsService: Deploy model");
        Date startTime = new Date();
        String errorMessage = "";
        try {
            fillInCldsModel(model);
            String bpmnJson = cldsBpmnTransformer.doXslTransformToString(model.getBpmnText());
            logger.info("PUT bpmnJson={}", bpmnJson);
            SecureServicePermission permisionManage = SecureServicePermission.create(cldsPermissionTypeClManage,
                cldsPermissionInstance, CldsEvent.ACTION_DEPLOY);
            isAuthorized(permisionManage);
            isAuthorizedForVf(model);
            ModelProperties modelProp = new ModelProperties(modelName, model.getControlName(), CldsEvent.ACTION_DEPLOY,
                false, bpmnJson, model.getPropText());
            checkForDuplicateServiceVf(modelName, model.getPropText());
            String deploymentId = "";
            // If model is already deployed then pass same deployment id
            if (model.getDeploymentId() != null && !model.getDeploymentId().isEmpty()) {
                deploymentId = model.getDeploymentId();
            } else {
                model.setDeploymentId(deploymentId = "closedLoop_" + UUID.randomUUID() + "_deploymentId");
            }
            model.setDeploymentStatusUrl(dcaeDispatcherServices.createNewDeployment(deploymentId, model.getTypeId(),
                modelProp.getGlobal().getDeployParameters()));
            CldsEvent.insEvent(cldsDao, model.getControlName(), getUserId(), CldsEvent.ACTION_DEPLOY,
                CldsEvent.ACTION_STATE_INITIATED, null);
            model.save(cldsDao, getUserId());
            // This is a blocking call
            checkDcaeDeploymentStatus(model, CldsEvent.ACTION_DEPLOY, true);
            // Refresh the model object in any cases for new event
            model = CldsModel.retrieve(cldsDao, model.getName(), false);
            // audit log
            LoggingUtils.setTimeContext(startTime, new Date());
            auditLogger.info("Deploy model completed");
        } catch (Exception e) {
            errorMessage = e.getMessage();
            logger.error("Exception occured during deployModel", e);
        }
        if (!errorMessage.isEmpty()) {
            model.setErrorMessageForUi(errorMessage);
            util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "DeployModel failed", Level.INFO,
                ONAPLogConstants.ResponseStatus.ERROR);
            return new ResponseEntity<>(model, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            util.exiting(HttpStatus.OK.toString(), "Successful", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
            return new ResponseEntity<>(model, HttpStatus.OK);
        }
    }

    public ResponseEntity<CldsModel> unDeployModel(String modelName, CldsModel model) {
        util.entering(request, "CldsService: Undeploy model");
        Date startTime = new Date();
        String errorMessage = "";
        try {
            SecureServicePermission permisionManage = SecureServicePermission.create(cldsPermissionTypeClManage,
                cldsPermissionInstance, CldsEvent.ACTION_UNDEPLOY);
            isAuthorized(permisionManage);
            isAuthorizedForVf(model);
            model.setDeploymentStatusUrl(
                dcaeDispatcherServices.deleteExistingDeployment(model.getDeploymentId(), model.getTypeId()));
            CldsEvent.insEvent(cldsDao, model.getControlName(), getUserId(), CldsEvent.ACTION_UNDEPLOY,
                CldsEvent.ACTION_STATE_INITIATED, null);
            // clean the deployment ID
            model.setDeploymentId(null);
            model.save(cldsDao, getUserId());
            // This is a blocking call
            checkDcaeDeploymentStatus(model, CldsEvent.ACTION_UNDEPLOY, true);
            // Refresh the model object in any cases for new event
            model = CldsModel.retrieve(cldsDao, model.getName(), false);
            // audit log
            LoggingUtils.setTimeContext(startTime, new Date());
            auditLogger.info("Undeploy model completed");
        } catch (Exception e) {
            errorMessage = e.getMessage();
            logger.error("Exception occured during unDeployModel", e);
        }
        if (!errorMessage.isEmpty()) {
            model.setErrorMessageForUi(errorMessage);
            util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "UndeployModel failed", Level.INFO,
                ONAPLogConstants.ResponseStatus.ERROR);
            return new ResponseEntity<>(model, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            util.exiting(HttpStatus.OK.toString(), "Successful", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
            return new ResponseEntity<>(model, HttpStatus.OK);
        }
    }

    private void checkDcaeDeploymentStatus(CldsModel model, String cldsEvent, boolean withRetry)
        throws InterruptedException {
        String operationStatus = withRetry
            ? dcaeDispatcherServices.getOperationStatusWithRetry(model.getDeploymentStatusUrl())
            : dcaeDispatcherServices.getOperationStatus(model.getDeploymentStatusUrl());
        if ("succeeded".equalsIgnoreCase(operationStatus)) {
            logger.info(cldsEvent + " model (" + model.getName() + ") succeeded...Deployment Id is - "
                + model.getDeploymentId());
            CldsEvent.insEvent(cldsDao, model.getControlName(), getUserId(), cldsEvent,
                CldsEvent.ACTION_STATE_COMPLETED, null);
        } else {
            String info = "DCAE " + cldsEvent + " (" + model.getName() + ") failed...Operation Status is - "
                + operationStatus;
            logger.info(info);
            CldsEvent.insEvent(cldsDao, model.getControlName(), getUserId(), cldsEvent, CldsEvent.ACTION_STATE_ERROR,
                null);
            util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "DCAE operation(" + cldsEvent + ") failed",
                Level.INFO, ONAPLogConstants.ResponseStatus.ERROR);
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, info);
        }
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

    // Created for the integration test
    public void setLoggingUtil(LoggingUtils utilP) {
        util = utilP;
    }
}