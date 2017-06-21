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
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.model.ValueItem;
import org.onap.clamp.clds.model.prop.ModelBpmn;
import org.onap.clamp.clds.transform.XslTransformer;
import org.camunda.bpm.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Service to save and retrieve the CLDS model attributes.
 */
@AjscService
@Path("/cldsTempate")
public class CldsTemplateService extends SecureServiceBase {

    private static final Logger logger = LoggerFactory.getLogger(CldsTemplateService.class);

    private static final String collectorKey = "Collector";
    private static final String stringMatchKey = "StringMatch";
    private static final String policyKey = "Policy";

    private static final String CLDS_PERMISSION_TYPE_TEMPLATE = System.getProperty("CLDS_PERMISSION_TYPE_TEMPLATE");
    private static final String CLDS_PERMISSION_INSTANCE = System.getProperty("CLDS_PERMISSION_INSTANCE");

    private static final SecureServicePermission PERMISSION_READ_TEMPLATE = SecureServicePermission.create(CLDS_PERMISSION_TYPE_TEMPLATE, CLDS_PERMISSION_INSTANCE, "read");
    private static final SecureServicePermission PERMISSION_UPDATE_TEMPLATE = SecureServicePermission.create(CLDS_PERMISSION_TYPE_TEMPLATE, CLDS_PERMISSION_INSTANCE, "update");

    @Autowired
    private CldsDao cldsDao;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private XslTransformer cldsBpmnTransformer;

    private static String userid;

    /**
     * REST service that retrieves BPMN for a CLDS template name from the database.
     * This is subset of the json getModel.
     * This is only expected to be used for testing purposes, not by the UI.
     *
     * @param templateName
     * @return bpmn xml text - content of bpmn given name
     */
    @GET
    @Path("/template/bpmn/{templateName}")
    @Produces(MediaType.TEXT_XML)
    public String getBpmnTemplate(@PathParam("templateName") String templateName) {
        isAuthorized(PERMISSION_READ_TEMPLATE);
        logger.info("GET bpmnText for templateName=" + templateName);
        CldsTemplate template = CldsTemplate.retrieve(cldsDao, templateName, false);
        return template.getBpmnText();
    }

    /**
     * REST service that saves BPMN for a CLDS template by name in the database.
     * This is subset of the json putModel.
     * This is only expected to be used for testing purposes, not by the UI.
     *
     * @param templateName
     * @param bpmnText
     */
    @PUT
    @Path("/template/bpmn/{templateName}")
    @Consumes(MediaType.TEXT_XML)
    public String putBpmnTemplateXml(@PathParam("templateName") String templateName, String bpmnText) {
        isAuthorized(PERMISSION_UPDATE_TEMPLATE);
        logger.info("PUT bpmnText for templateName=" + templateName);
        logger.info("PUT bpmnText=" + bpmnText);
        CldsTemplate cldsTemplate = CldsTemplate.retrieve(cldsDao, templateName, true);
        cldsTemplate.setBpmnText(bpmnText);
        cldsTemplate.save(cldsDao, userid);
        return "wrote bpmnText for templateName=" + templateName;
    }

    /**
     * REST service that retrieves image for a CLDS template name from the database.
     * This is subset of the json getModel.
     * This is only expected to be used for testing purposes, not by the UI.
     *
     * @param templateName
     * @return image xml text - content of image given name
     */
    @GET
    @Path("/template/image/{templateName}")
    @Produces(MediaType.TEXT_XML)
    public String getImageXml(@PathParam("templateName") String templateName) {
        isAuthorized(PERMISSION_READ_TEMPLATE);
        logger.info("GET imageText for templateName=" + templateName);
        CldsTemplate template = CldsTemplate.retrieve(cldsDao, templateName, false);
        return template.getImageText();
    }

    /**
     * REST service that saves image for a CLDS template by name in the database.
     * This is subset of the json putModel.
     * This is only expected to be used for testing purposes, not by the UI.
     *
     * @param templateName
     * @param imageText
     */
    @PUT
    @Path("/template/image/{templateName}")
    @Consumes(MediaType.TEXT_XML)
    public String putImageXml(@PathParam("templateName") String templateName, String imageText) {
        isAuthorized(PERMISSION_UPDATE_TEMPLATE);
        logger.info("PUT iamgeText for modelName=" + templateName);
        logger.info("PUT imageText=" + imageText);
        CldsTemplate cldsTemplate = CldsTemplate.retrieve(cldsDao, templateName, true);
        cldsTemplate.setImageText(imageText);
        cldsTemplate.save(cldsDao, userid);
        return "wrote imageText for modelName=" + templateName;
    }

    /**
     * REST service that retrieves a CLDS template by name from the database.
     *
     * @param templateName
     * @return clds template - clds template for the given template name
     */
    @GET
    @Path("/template/{templateName}")
    @Produces(MediaType.APPLICATION_JSON)
    public CldsTemplate getTemplate(@PathParam("templateName") String templateName) {
        isAuthorized(PERMISSION_READ_TEMPLATE);
        logger.info("GET model for  templateName=" + templateName);
        return CldsTemplate.retrieve(cldsDao, templateName, false);
    }

    /**
     * REST service that saves a CLDS template by name in the database.
     *
     * @param templateName
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @PUT
    @Path("/template/{templateName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CldsTemplate putTemplate(@PathParam("templateName") String templateName, CldsTemplate cldsTemplate) throws TransformerException, IOException {
        isAuthorized(PERMISSION_UPDATE_TEMPLATE);
        logger.info("PUT Template for  templateName=" + templateName);
        logger.info("PUT bpmnText=" + cldsTemplate.getBpmnText());
        logger.info("PUT propText=" + cldsTemplate.getPropText());
        logger.info("PUT imageText=" + cldsTemplate.getImageText());
        cldsTemplate.setName(templateName);
        String bpmnText = cldsTemplate.getBpmnText();
        String imageText = cldsTemplate.getImageText();
        String propText = cldsTemplate.getPropText();
        Map<String, String> newBpmnIdsMap = getNewBpmnIdsMap(bpmnText, cldsTemplate.getPropText());
        for (String currBpmnId : newBpmnIdsMap.keySet()) {
            if (currBpmnId != null && newBpmnIdsMap.get(currBpmnId) != null) {
                bpmnText = bpmnText.replace(currBpmnId, newBpmnIdsMap.get(currBpmnId));
                imageText = imageText.replace(currBpmnId, newBpmnIdsMap.get(currBpmnId));
                propText = propText.replace(currBpmnId, newBpmnIdsMap.get(currBpmnId));
            }
        }
        cldsTemplate.setBpmnText(bpmnText);
        cldsTemplate.setImageText(imageText);
        cldsTemplate.setPropText(propText);
        logger.info(" bpmnText : " + cldsTemplate.getBpmnText());
        logger.info(" Image Text : " + cldsTemplate.getImageText());
        logger.info(" Prop Text : " + cldsTemplate.getPropText());
        cldsTemplate.save(cldsDao, userid);
        return cldsTemplate;
    }

    /**
     * REST service that retrieves a list of CLDS template names.
     *
     * @return template names in JSON
     */
    @GET
    @Path("/template-names")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ValueItem> getTemplateNames() {
        isAuthorized(PERMISSION_READ_TEMPLATE);
        logger.info("GET list of template names");
        return cldsDao.getTemplateNames();
    }


    private Map<String, String> getNewBpmnIdsMap(String bpmnText, String propText) throws TransformerException, IOException {
        /**
         *  Test sample code start
         */
        String bpmnJson = cldsBpmnTransformer.doXslTransformToString(bpmnText);
        ModelBpmn templateBpmn = ModelBpmn.create(bpmnJson);
        List<String> bpmnElementIds = templateBpmn.getBpmnElementIds();
        logger.info("value of elementIds:" + bpmnElementIds);
        logger.info("value of prop text:" + propText);
        Map<String, String> bpmnIoIdsMap = new HashMap<>();
        if (bpmnElementIds != null && bpmnElementIds.size() > 0) {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode root = objectMapper.readValue(propText, ObjectNode.class);
            Iterator<Entry<String, JsonNode>> entryItr = root.fields();
            while (entryItr.hasNext()) {
                // process the entry
                Entry<String, JsonNode> entry = entryItr.next();
                String keyPropName = entry.getKey();
                for (String currElementId : bpmnElementIds) {
                    if (keyPropName != null && keyPropName.equalsIgnoreCase(currElementId)) {
                        ArrayNode arrayNode = (ArrayNode) entry.getValue();
                        // process each id/from object, like: {"id":"Collector_11r50j1", "from":"StartEvent_1"}
                        for (JsonNode anArrayNode : arrayNode) {
                            ObjectNode node = (ObjectNode) anArrayNode;
                            String valueNode = node.get("value").asText();
                            logger.info("value of node:" + valueNode);
                            if (keyPropName.startsWith(collectorKey)) {
                                valueNode = collectorKey + "_" + valueNode;
                            } else if (keyPropName.startsWith(stringMatchKey)) {
                                valueNode = stringMatchKey + "_" + valueNode;
                            } else if (keyPropName.startsWith(policyKey)) {
                                valueNode = policyKey + "_" + valueNode;
                            }
                            bpmnIoIdsMap.put(keyPropName, valueNode);
                        }
                        break;
                    }
                }
            }
        }
        logger.info("value of hashmap:" + bpmnIoIdsMap);
        /**
         *  Test sample code end
         */
        return bpmnIoIdsMap;
    }
}
