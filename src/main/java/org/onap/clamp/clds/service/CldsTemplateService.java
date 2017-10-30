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

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.transform.TransformerException;

import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.model.ValueItem;
import org.onap.clamp.clds.transform.XslTransformer;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Service to save and retrieve the CLDS model attributes.
 */
@AjscService
@Path("/cldsTempate")
public class CldsTemplateService extends SecureServiceBase {

    @Value("${CLDS_PERMISSION_TYPE_TEMPLATE:permission-type-template}")
    private String                  cldsPermissionTypeTemplate;

    @Value("${CLDS_PERMISSION_INSTANCE:dev}")
    private String                  cldsPermissionInstance;

    private SecureServicePermission permissionReadTemplate;

    private SecureServicePermission permissionUpdateTemplate;

    @PostConstruct
    private final void afterConstruction() {
        permissionReadTemplate = SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance,
                "read");
        permissionUpdateTemplate = SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance,
                "update");
    }

    @Autowired
    private CldsDao        cldsDao;

    @Autowired
    private XslTransformer cldsBpmnTransformer;

    private static String  userid;

    /**
     * REST service that retrieves BPMN for a CLDS template name from the
     * database. This is subset of the json getModel. This is only expected to
     * be used for testing purposes, not by the UI.
     *
     * @param templateName
     * @return bpmn xml text - content of bpmn given name
     */
    @GET
    @Path("/template/bpmn/{templateName}")
    @Produces(MediaType.TEXT_XML)
    public String getBpmnTemplate(@PathParam("templateName") String templateName) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsTemplateService: GET template bpmn", getPrincipalName());
        isAuthorized(permissionReadTemplate);
        logger.info("GET bpmnText for templateName=" + templateName);
        CldsTemplate template = CldsTemplate.retrieve(cldsDao, templateName, false);
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get template bpmn success", this.getClass().getName());
        auditLogger.info("GET template bpmn completed");
        return template.getBpmnText();
    }

    /**
     * REST service that retrieves image for a CLDS template name from the
     * database. This is subset of the json getModel. This is only expected to
     * be used for testing purposes, not by the UI.
     *
     * @param templateName
     * @return image xml text - content of image given name
     */
    @GET
    @Path("/template/image/{templateName}")
    @Produces(MediaType.TEXT_XML)
    public String getImageXml(@PathParam("templateName") String templateName) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsTemplateService: GET template image", getPrincipalName());
        isAuthorized(permissionReadTemplate);
        logger.info("GET imageText for templateName=" + templateName);
        CldsTemplate template = CldsTemplate.retrieve(cldsDao, templateName, false);
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get template image success", this.getClass().getName());
        auditLogger.info("GET template image completed");
        return template.getImageText();
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
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsTemplateService: GET template", getPrincipalName());
        isAuthorized(permissionReadTemplate);
        logger.info("GET model for  templateName=" + templateName);
        CldsTemplate template = CldsTemplate.retrieve(cldsDao, templateName, false);
        template.setUserAuthorizedToUpdate(isAuthorizedNoException(permissionUpdateTemplate));
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get template success", this.getClass().getName());
        auditLogger.info("GET template completed");
        return template;
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
    public CldsTemplate putTemplate(@PathParam("templateName") String templateName, CldsTemplate cldsTemplate)
            throws TransformerException, IOException {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsTemplateService: PUT template", getPrincipalName());
        isAuthorized(permissionUpdateTemplate);

        logger.info("PUT Template for  templateName=" + templateName);
        logger.info("PUT bpmnText=" + cldsTemplate.getBpmnText());
        logger.info("PUT propText=" + cldsTemplate.getPropText());
        logger.info("PUT imageText=" + cldsTemplate.getImageText());
        cldsTemplate.setName(templateName);
        String bpmnText = cldsTemplate.getBpmnText();
        String imageText = cldsTemplate.getImageText();
        String propText = cldsTemplate.getPropText();
        cldsTemplate.setBpmnText(bpmnText);
        cldsTemplate.setImageText(imageText);
        cldsTemplate.setPropText(propText);
        logger.info(" bpmnText : " + cldsTemplate.getBpmnText());
        logger.info(" Image Text : " + cldsTemplate.getImageText());
        logger.info(" Prop Text : " + cldsTemplate.getPropText());
        cldsTemplate.save(cldsDao, userid);

        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Put template success", this.getClass().getName());
        auditLogger.info("PUT template completed");

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
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsTemplateService: GET template names", getPrincipalName());
        isAuthorized(permissionReadTemplate);
        logger.info("GET list of template names");
        List<ValueItem> names = cldsDao.getTemplateNames();
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get template names success", this.getClass().getName());
        auditLogger.info("GET template names completed");
        return names;
    }
}
