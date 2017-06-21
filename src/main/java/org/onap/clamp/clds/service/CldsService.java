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
import com.att.ajsc.filemonitor.AJSCPropertiesMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onap.clamp.clds.client.SdcCatalogServices;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.clamp.clds.transform.XslTransformer;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.jboss.resteasy.spi.BadRequestException;
import org.onap.clamp.clds.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Service to save and retrieve the CLDS model attributes.
 */
@AjscService
@Path("/clds")
public class CldsService extends SecureServiceBase {

    @Autowired
    private ApplicationContext appContext;

    private static final Logger logger = LoggerFactory.getLogger(CldsService.class);

    @Value("${CLDS_PERMISSION_TYPE_CL:org.onap.clamp.clds.cl}")
    private static String CLDS_PERMISSION_TYPE_CL;

    @Value("${CLDS_PERMISSION_TYPE_CL_MANAGE:org.onap.clamp.clds.cl.manage}")
    private static String CLDS_PERMISSION_TYPE_CL_MANAGE;

    @Value("${CLDS_PERMISSION_TYPE_CL_EVENT:/META-INF/securityFilterRules.json}")
    private static String CLDS_PERMISSION_TYPE_CL_EVENT;

    @Value("${CLDS_PERMISSION_TYPE_FILTER_VF:/META-INF/securityFilterRules.json}")
    private static String CLDS_PERMISSION_TYPE_FILTER_VF;

    @Value("${CLDS_PERMISSION_INSTANCE:/META-INF/securityFilterRules.json}")
    private static String CLDS_PERMISSION_INSTANCE;

    private static final SecureServicePermission PERMISSION_READ_CL = SecureServicePermission.create(CLDS_PERMISSION_TYPE_CL, CLDS_PERMISSION_INSTANCE, "read");

    private static final SecureServicePermission PERMISSION_UPDATE_CL = SecureServicePermission.create(CLDS_PERMISSION_TYPE_CL, CLDS_PERMISSION_INSTANCE, "update");

    @Value("${org.onap.clamp.config.files.globalClds:classpath:/clds/globalClds.properties}")
    private String globalClds;
    private Properties globalCldsProperties;

    @Autowired
    private CldsDao cldsDao;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private XslTransformer cldsBpmnTransformer;

    @Autowired
    private RefProp refProp;

    @Autowired
    private SdcCatalogServices asdcCatalogServices;
    //

    public CldsService() {
    }

    public CldsService(RefProp refProp) {
        this.refProp = refProp;
    }

    /**
     * REST service that retrieves BPMN for a CLDS model name from the database.
     * This is subset of the json getModel.
     * This is only expected to be used for testing purposes, not by the UI.
     *
     * @param modelName
     * @return bpmn xml text - content of bpmn given name
     */
    @GET
    @Path("/model/bpmn/{modelName}")
    @Produces(MediaType.TEXT_XML)
    public String getBpmnXml(@PathParam("modelName") String modelName) {
        isAuthorized(PERMISSION_READ_CL);
        logger.info("GET bpmnText for modelName={}", modelName);
        CldsModel model = CldsModel.retrieve(cldsDao, modelName, false);
        return model.getBpmnText();
    }

    /**
     * REST service that saves BPMN for a CLDS model by name in the database.
     * This is subset of the json putModel.
     * This is only expected to be used for testing purposes, not by the UI.
     *
     * @param modelName
     */
    @PUT
    @Path("/model/bpmn/{modelName}")
    @Consumes(MediaType.TEXT_XML)
    public String putBpmnXml(@PathParam("modelName") String modelName, String bpmnText) {
        isAuthorized(PERMISSION_UPDATE_CL);
        logger.info("PUT bpmnText for modelName={}", modelName);
        logger.info("PUT bpmnText={}", bpmnText);
        CldsModel cldsModel = CldsModel.retrieve(cldsDao, modelName, true);
        cldsModel.setBpmnText(bpmnText);
        cldsModel.save(cldsDao, getUserid());
        return "wrote bpmnText for modelName=" + modelName;
    }

    /**
     * REST service that retrieves image for a CLDS model name from the database.
     * This is subset of the json getModel.
     * This is only expected to be used for testing purposes, not by the UI.
     *
     * @param modelName
     * @return image xml text - content of image given name
     */
    @GET
    @Path("/model/image/{modelName}")
    @Produces(MediaType.TEXT_XML)
    public String getImageXml(@PathParam("modelName") String modelName) {
        isAuthorized(PERMISSION_READ_CL);
        logger.info("GET imageText for modelName={}", modelName);
        CldsModel model = CldsModel.retrieve(cldsDao, modelName, false);
        return model.getImageText();
    }

    /**
     * REST service that saves image for a CLDS model by name in the database.
     * This is subset of the json putModel.
     * This is only expected to be used for testing purposes, not by the UI.
     *
     * @param modelName
     */
    @PUT
    @Path("/model/image/{modelName}")
    @Consumes(MediaType.TEXT_XML)
    public String putImageXml(@PathParam("modelName") String modelName, String imageText) {
        isAuthorized(PERMISSION_UPDATE_CL);
        logger.info("PUT iamgeText for modelName={}", modelName);
        logger.info("PUT imageText={}", imageText);
        CldsModel cldsModel = CldsModel.retrieve(cldsDao, modelName, true);
        cldsModel.setImageText(imageText);
        cldsModel.save(cldsDao, getUserid());
        return "wrote imageText for modelName=" + modelName;
    }

    /**
     * REST service that retrieves a CLDS model by name from the database.
     *
     * @param modelName
     * @return clds model - clds model for the given model name
     * @throws NotAuthorizedException
     */
    @GET
    @Path("/model/{modelName}")
    @Produces(MediaType.APPLICATION_JSON)
    public CldsModel getModel(@PathParam("modelName") String modelName) throws NotAuthorizedException {
        isAuthorized(PERMISSION_READ_CL);
        logger.debug("GET model for  modelName={}", modelName);
        CldsModel cldsModel = CldsModel.retrieve(cldsDao, modelName, false);
        isAuthorizedForVf(cldsModel);
        return cldsModel;
    }

    /**
     * REST service that saves a CLDS model by name in the database.
     *
     * @param modelName
     * @throws TransformerException
     * @throws TransformerConfigurationException
     */
    @PUT
    @Path("/model/{modelName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CldsModel putModel(@PathParam("modelName") String modelName, CldsModel cldsModel) throws TransformerException {
        isAuthorized(PERMISSION_UPDATE_CL);
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
        cldsModel.save(cldsDao, getUserid());
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
//		isAuthorized(PERMISSION_READ_CL);
        logger.info("GET list of model names");
        return cldsDao.getBpmnNames();
    }

    /**
     * REST service that saves and processes an action for a CLDS model by name.
     *
     * @param action
     * @param modelName
     * @param test
     * @param model
     * @return
     * @throws TransformerConfigurationException
     * @throws TransformerException
     * @throws IOException
     * @throws JsonProcessingException
     * @throws NotAuthorizedException
     */
    @PUT
    @Path("/action/{action}/{modelName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CldsModel putModelAndProcessAction(@PathParam("action") String action, @PathParam("modelName") String modelName, @QueryParam("test") String test, CldsModel model) throws TransformerException, NotAuthorizedException, IOException {
        String actionCd = action.toUpperCase();
        SecureServicePermission permisionManage = SecureServicePermission.create(CLDS_PERMISSION_TYPE_CL_MANAGE, CLDS_PERMISSION_INSTANCE, actionCd);
        isAuthorized(permisionManage);
        isAuthorizedForVf(model);
        String userid = getUserid();
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
        model.save(cldsDao, getUserid());

        // get vars and format if necessary
        String prop = model.getPropText();
        String bpmn = model.getBpmnText();
        String docText = model.getDocText();
        String controlName = model.getControlName();

        String bpmnJson = cldsBpmnTransformer.doXslTransformToString(bpmn);
        logger.info("PUT bpmnJson={}", bpmnJson);

        boolean isTest = false;
        if (test != null && test.equalsIgnoreCase("true")) {
            isTest = true;
        } else {
            // if action.test.override is true, then any action will be marked as test=true (even if incoming action request had test=false); otherwise, test flag will be unchanged on the action request
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
        variables.put("modelProp", prop);
        variables.put("modelBpmnProp", bpmnJson);
        variables.put("modelName", modelName);
        variables.put("controlName", controlName);
        variables.put("docText", docText);
        variables.put("isTest", isTest);
        variables.put("userid", userid);
        variables.put("isInsertTestEvent", isInsertTestEvent);

        // start camunda process
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);

        // log process info
        logger.info("Started processDefinitionId={}, processInstanceId={}", pi.getProcessDefinitionId(), pi.getProcessInstanceId());

        // refresh model info from db (get fresh event info)
        return CldsModel.retrieve(cldsDao, modelName, false);
    }

    /**
     * REST service that accepts events for a model.
     *
     * @param test
     * @param dcaeEvent
     * @throws BadRequestException
     */
    @POST
    @Path("/dcae/event")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String postDcaeEvent(@QueryParam("test") String test, DcaeEvent dcaeEvent) throws BadRequestException {
        String userid = null;
        // TODO: allow auth checking to be turned off by removing the permission type property
        if (CLDS_PERMISSION_TYPE_CL_EVENT != null && CLDS_PERMISSION_TYPE_CL_EVENT.length() > 0) {
            SecureServicePermission permissionEvent = SecureServicePermission.create(CLDS_PERMISSION_TYPE_CL_EVENT, CLDS_PERMISSION_INSTANCE, dcaeEvent.getEvent());
            isAuthorized(permissionEvent);
            userid = getUserid();
        }

        boolean isTest = false;
        if (test != null && test.equalsIgnoreCase("true")) {
            isTest = true;
        }

        int instanceCount = 0;
        if (dcaeEvent.getInstances() != null) {
            instanceCount = dcaeEvent.getInstances().size();
        }
        String msgInfo = "event=" + dcaeEvent.getEvent() + " serviceUUID=" + dcaeEvent.getServiceUUID() + " resourceUUID=" + dcaeEvent.getResourceUUID() + " artifactName=" + dcaeEvent.getArtifactName() + " instance count=" + instanceCount + " isTest=" + isTest;
        logger.info("POST dcae event {}", msgInfo);

        if (isTest) {
            logger.warn("Ignorning test event from DCAE");
        } else {
            if (DcaeEvent.EVENT_DEPLOYMENT.equalsIgnoreCase(dcaeEvent.getEvent())) {
                CldsModel.insertModelInstance(cldsDao, dcaeEvent, userid);
            } else {
                CldsEvent.insEvent(cldsDao, dcaeEvent.getControlName(), userid, dcaeEvent.getCldsActionCd(), CldsEvent.ACTION_STATE_RECEIVED, null);
            }
            // EVENT_UNDEPLOYMENT is defunct - DCAE Proxy will not undeploy individual instances.  It will send an empty list of
            // deployed instances to indicate all have been removed.  Or it will send an updated list to indicate those that
            // are still deployed with any not on the list considered undeployed.
            //else if(DcaeEvent.EVENT_UNDEPLOYMENT.equalsIgnoreCase(dcaeEvent.getEvent()))
            //{
            //	CldsModel.removeModelInstance(cldsDao, dcaeEvent);
            //}
        }

        return msgInfo;
    }

    /**
     * REST service that retrieves asdc services
     *
     * @throws Exception
     */
    @GET
    @Path("/asdc/services")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAsdcServices() throws Exception {
        String retStr;
        try {
            String responseStr = asdcCatalogServices.getAsdcServicesInformation(null);
            retStr = createUiServiceFormatJson(responseStr);
        } catch (Exception e) {
            logger.info("{} {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
        logger.info("value of asdcServices : {}", retStr);
        return retStr;
    }

    /**
     * REST service that retrieves total properties required by UI
     *
     * @throws Exception
     */
    @GET
    @Path("/properties")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAsdcProperties() throws Exception {
        return createPropertiesObjectByUUID(getGlobalCldsString(), "{}");
    }

    /**
     * REST service that retrieves total properties by using invariantUUID based on refresh and non refresh
     *
     * @throws Exception
     */
    @GET
    @Path("/properties/{serviceInvariantUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAsdcPropertiesByServiceUUIDForRefresh(@PathParam("serviceInvariantUUID") String serviceInvariantUUID, @DefaultValue("false") @QueryParam("refresh") String refresh) throws Exception {
        CldsServiceData cldsServiceData = new CldsServiceData();
        cldsServiceData.setServiceInvariantUUID(serviceInvariantUUID);

        boolean isCldsAsdcDataExpired = true;
        //  To getcldsService information from database cache using invariantUUID only when refresh = false
        if (refresh != null && refresh.equalsIgnoreCase("false")) {
            cldsServiceData = cldsServiceData.getCldsServiceCache(cldsDao, serviceInvariantUUID);
            // If cldsService is available in database Cache , verify is data expired or not
            if (cldsServiceData != null) {
                isCldsAsdcDataExpired = asdcCatalogServices.isCldsAsdcCacheDataExpired(cldsServiceData);
            }
        }
        // If user Requested for refresh or database cache expired , get all data from asdc api.
        if ((refresh != null && refresh.equalsIgnoreCase("true")) || isCldsAsdcDataExpired) {
            cldsServiceData = asdcCatalogServices.getCldsServiceDataWithAlarmConditions(serviceInvariantUUID);
            CldsDBServiceCache cldsDBServiceCache = asdcCatalogServices.getCldsDBServiceCacheUsingCldsServiceData(cldsServiceData);
            if (cldsDBServiceCache != null && cldsDBServiceCache.getInvariantId() != null && cldsDBServiceCache.getServiceId() != null) {
                cldsServiceData.setCldsServiceCache(cldsDao, cldsDBServiceCache);
            }
        }

        // filter out VFs the user is not authorized for
        cldsServiceData.filterVfs(this);

        // format retrieved data into properties json
        return asdcCatalogServices.createPropertiesObjectByUUID(getGlobalCldsString(), cldsServiceData);
    }

    /**
     * Determine if the user is authorized for a particular VF by its invariant UUID.
     *
     * @param vfInvariantUuid
     * @throws NotAuthorizedException
     * @return
     */
    public boolean isAuthorizedForVf(String vfInvariantUuid) throws NotAuthorizedException {
        if (CLDS_PERMISSION_TYPE_FILTER_VF != null && CLDS_PERMISSION_TYPE_FILTER_VF.length() > 0) {
            SecureServicePermission permission = SecureServicePermission.create(CLDS_PERMISSION_TYPE_FILTER_VF, CLDS_PERMISSION_INSTANCE, vfInvariantUuid);
            return isAuthorized(permission);
        } else {
            // if CLDS_PERMISSION_TYPE_FILTER_VF property is not provided, then VF filtering is turned off
            logger.warn("VF filtering turned off");
            return true;
        }
    }

    /**
     * Determine if the user is authorized for a particular VF by its invariant UUID.
     * If not authorized, then NotAuthorizedException is thrown.
     *
     * @param model
     * @return
     */
    private boolean isAuthorizedForVf(CldsModel model) throws NotAuthorizedException {
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
        List<CldsAsdcServiceInfo> rawList = objectMapper.readValue(responseStr, objectMapper.getTypeFactory().constructCollectionType(List.class, CldsAsdcServiceInfo.class));
        ObjectNode invariantIdServiceNode = objectMapper.createObjectNode();
        ObjectNode serviceNode = objectMapper.createObjectNode();
        logger.info("value of cldsserviceiNfolist: {}", rawList);
        if (rawList != null && rawList.size() > 0) {
            List<CldsAsdcServiceInfo> cldsAsdcServiceInfoList = asdcCatalogServices.removeDuplicateServices(rawList);

            for (CldsAsdcServiceInfo currCldsAsdcServiceInfo : cldsAsdcServiceInfoList) {
                if (currCldsAsdcServiceInfo != null) {
                    invariantIdServiceNode.put(currCldsAsdcServiceInfo.getInvariantUUID(), currCldsAsdcServiceInfo.getName());
                }
            }
            serviceNode.putPOJO("service", invariantIdServiceNode);
        }
        return serviceNode.toString();
    }

    private String createPropertiesObjectByUUID(String globalProps, String cldsResponseStr) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        CldsAsdcServiceDetail cldsAsdcServiceDetail = mapper.readValue(cldsResponseStr, CldsAsdcServiceDetail.class);
        ObjectNode globalPropsJson = null;
        if (cldsAsdcServiceDetail != null && cldsAsdcServiceDetail.getUuid() != null) {
            /**
             * to create json with vf, alarm and locations
             */
            ObjectNode serviceObjectNode = createEmptyVfAlarmObject(mapper);
            ObjectNode vfObjectNode = mapper.createObjectNode();

            /**
             * to create json with vf and vfresourceId
             */
            createVfObjectNode(vfObjectNode, mapper, cldsAsdcServiceDetail.getResources());
            serviceObjectNode.putPOJO(cldsAsdcServiceDetail.getInvariantUUID(), vfObjectNode);
            ObjectNode byServiceBasicObjetNode = mapper.createObjectNode();
            byServiceBasicObjetNode.putPOJO("byService", serviceObjectNode);

            /**
             * to create json with VFC Node
             */
            ObjectNode emptyvfcobjectNode = createByVFCObjectNode(mapper, cldsAsdcServiceDetail.getResources());
            byServiceBasicObjetNode.putPOJO("byVf", emptyvfcobjectNode);
            globalPropsJson = (ObjectNode) mapper.readValue(globalProps, JsonNode.class);
            globalPropsJson.putPOJO("shared", byServiceBasicObjetNode);
            logger.info("valuie of objNode: {}", globalPropsJson);
        } else {
            /**
             *  to create json with total properties when no serviceUUID passed
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

    private void createVfObjectNode(ObjectNode vfObjectNode2, ObjectMapper mapper, List<CldsAsdcResource> rawCldsAsdcResourceList) throws IOException {
        ObjectNode vfNode = mapper.createObjectNode();
        vfNode.put("", "");

        // To remove repeated resource instance name from resourceInstanceList
        List<CldsAsdcResource> cldsAsdcResourceList = asdcCatalogServices.removeDuplicateAsdcResourceInstances(rawCldsAsdcResourceList);
        /**
         * Creating vf resource node using cldsAsdcResource Object
         */
        if (cldsAsdcResourceList != null && cldsAsdcResourceList.size() > 0) {
            for (CldsAsdcResource cldsAsdcResource : cldsAsdcResourceList) {
                if (cldsAsdcResource != null && cldsAsdcResource.getResoucreType() != null && cldsAsdcResource.getResoucreType().equalsIgnoreCase("VF")) {
                    vfNode.put(cldsAsdcResource.getResourceUUID(), cldsAsdcResource.getResourceName());
                }
            }
        }
        vfObjectNode2.putPOJO("vf", vfNode);
        String locationStringValue = refProp.getStringValue("ui.location.default");
        String alarmStringValue = refProp.getStringValue("ui.alarm.default");

        /**
         *  creating location json object using properties file value
         */
        ObjectNode locationJsonNode = (ObjectNode) mapper.readValue(locationStringValue, JsonNode.class);
        vfObjectNode2.putPOJO("location", locationJsonNode);

        /**
         * creating alarm json object using properties file value
         */
        logger.info("value of alarm: {}", alarmStringValue);
        ObjectNode alarmStringJsonNode = (ObjectNode) mapper.readValue(alarmStringValue, JsonNode.class);
        vfObjectNode2.putPOJO("alarmCondition", alarmStringJsonNode);
    }

    private ObjectNode createByVFCObjectNode(ObjectMapper mapper, List<CldsAsdcResource> cldsAsdcResourceList) {
        ObjectNode emptyObjectNode = mapper.createObjectNode();
        ObjectNode emptyvfcobjectNode = mapper.createObjectNode();
        ObjectNode vfCObjectNode = mapper.createObjectNode();
        vfCObjectNode.putPOJO("vfC", emptyObjectNode);
        ObjectNode subVfCObjectNode = mapper.createObjectNode();
        subVfCObjectNode.putPOJO("vfc", emptyObjectNode);
        if (cldsAsdcResourceList != null && cldsAsdcResourceList.size() > 0) {
            for (CldsAsdcResource cldsAsdcResource : cldsAsdcResourceList) {
                if (cldsAsdcResource != null && cldsAsdcResource.getResoucreType() != null && cldsAsdcResource.getResoucreType().equalsIgnoreCase("VF")) {
                    vfCObjectNode.putPOJO(cldsAsdcResource.getResourceUUID(), subVfCObjectNode);
                }
            }
        }
        emptyvfcobjectNode.putPOJO("", vfCObjectNode);
        return emptyvfcobjectNode;
    }
    
    private String getGlobalCldsString() throws Exception {
    	if  (null == globalCldsProperties) {
            globalCldsProperties = new Properties();
            globalCldsProperties.load(appContext.getResource(globalClds).getInputStream());
    	}
    	return (String) globalCldsProperties.get("globalCldsProps");
    }
}
