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

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.model.ValueItem;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.clamp.clds.util.ONAPLogConstants;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service to save and retrieve the CLDS model attributes.
 */
@Component
public class CldsTemplateService extends SecureServiceBase {

    @Value("${clamp.config.security.permission.type.template:permission-type-template}")
    private String cldsPermissionTypeTemplate;
    @Value("${clamp.config.security.permission.instance:dev}")
    private String cldsPermissionInstance;
    private SecureServicePermission permissionReadTemplate;
    private SecureServicePermission permissionUpdateTemplate;
    @Autowired
	private HttpServletRequest request;

    @PostConstruct
    private final void afterConstruction() {
        permissionReadTemplate = SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance,
            "read");
        permissionUpdateTemplate = SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance,
            "update");
    }

    @Autowired
    private CldsDao cldsDao;
    private LoggingUtils util = new LoggingUtils(logger);

    /**
     * REST service that retrieves BPMN for a CLDS template name from the
     * database. This is subset of the json getModel. This is only expected to
     * be used for testing purposes, not by the UI.
     *
     * @param templateName
     * @return bpmn xml text - content of bpmn given name
     */
    public String getBpmnTemplate(String templateName) {
    	util.entering(request, "CldsTemplateService: GET template bpmn");
        Date startTime = new Date();
        isAuthorized(permissionReadTemplate);
        logger.info("GET bpmnText for templateName=" + templateName);
        CldsTemplate template = CldsTemplate.retrieve(cldsDao, templateName, false);
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("GET template bpmn completed");
        util.exiting("200", "Get template bpmn success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
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
    public String getImageXml(String templateName) {
    	util.entering(request, "CldsTemplateService: GET template image");
        Date startTime = new Date();
        isAuthorized(permissionReadTemplate);
        logger.info("GET imageText for templateName=" + templateName);
        CldsTemplate template = CldsTemplate.retrieve(cldsDao, templateName, false);
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("GET template image completed");
        util.exiting("200", "Get template image success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return template.getImageText();
    }

    /**
     * REST service that retrieves a CLDS template by name from the database.
     *
     * @param templateName
     * @return clds template - clds template for the given template name
     */
    public CldsTemplate getTemplate(String templateName) {
    	util.entering(request, "CldsTemplateService: GET template");
        Date startTime = new Date();
        isAuthorized(permissionReadTemplate);
        logger.info("GET model for  templateName=" + templateName);
        CldsTemplate template = CldsTemplate.retrieve(cldsDao, templateName, false);
        template.setUserAuthorizedToUpdate(isAuthorizedNoException(permissionUpdateTemplate));
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("GET template completed");
        util.exiting("200", "Get template success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return template;
    }

    /**
     * REST service that saves a CLDS template by name in the database.
     *
     * @param templateName
     * @param cldsTemplate
     * @return The CldsTemplate modified and saved in DB
     */
    public CldsTemplate putTemplate(String templateName, CldsTemplate cldsTemplate) {
    	util.entering(request, "CldsTemplateService: PUT template");
        Date startTime = new Date();
        isAuthorized(permissionUpdateTemplate);
        logger.info("PUT Template for  templateName=" + templateName);
        logger.info("PUT bpmnText=" + cldsTemplate.getBpmnText());
        logger.info("PUT propText=" + cldsTemplate.getPropText());
        logger.info("PUT imageText=" + cldsTemplate.getImageText());
        cldsTemplate.setName(templateName);
        cldsTemplate.save(cldsDao, null);
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("PUT template completed");
        util.exiting("200", "Put template success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return cldsTemplate;
    }

    /**
     * REST service that retrieves a list of CLDS template names.
     *
     * @return template names in JSON
     */
    public List<ValueItem> getTemplateNames() {
    	util.entering(request, "CldsTemplateService: GET template names");
        Date startTime = new Date();
        isAuthorized(permissionReadTemplate);
        logger.info("GET list of template names");
        List<ValueItem> names = cldsDao.getTemplateNames();
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("GET template names completed");
        util.exiting("200", "Get template names success", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return names;
    }

    // Created for the integration test
    public void setLoggingUtil (LoggingUtils utilP) {
        util =  utilP;
    }
}
