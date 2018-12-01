/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsToscaModel;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * REST services to manage Tosca Model
 */
@Component
public class CldsToscaService extends SecureServiceBase {

    @Value("${clamp.config.security.permission.type.tosca:permission-type-tosca}")
    private String                  cldsPermissionTypeTosca;
    @Value("${clamp.config.security.permission.instance:dev}")
    private String                  cldsPermissionInstance;
    private SecureServicePermission permissionReadTosca;
    private SecureServicePermission permissionUpdateTosca;

    @Autowired
    private CldsDao                 cldsDao;

    @Autowired
    private ClampProperties         refProp;

    @Autowired
    private PolicyClient            policyClient;
    private LoggingUtils util = new LoggingUtils(logger);

    @PostConstruct
    private final void initConstruct() {
        permissionReadTosca = SecureServicePermission.create(cldsPermissionTypeTosca, cldsPermissionInstance, "read");
        permissionUpdateTosca = SecureServicePermission.create(cldsPermissionTypeTosca, cldsPermissionInstance,
                "update");
    }

    /**
     * REST service to upload a new Tosca Model or update an existing Tosca
     * model with new version. This API will parse the Tosca model yaml and
     * generates a JSON schema out of it.
     * 
     * @param toscaModelName
     *            Tosca model name to be used as a key
     * @param cldsToscaModel
     *            Object containing the tosca model yaml
     * 
     * @return clds tosca models - list of CLDS tosca models for a given policy
     *         type
     */
    public ResponseEntity<?> parseToscaModelAndSave(String toscaModelName, CldsToscaModel cldsToscaModel) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsToscaService: Parse Tosca model and save", getPrincipalName());
        // TODO revisit based on new permissions
        isAuthorized(permissionUpdateTosca);
        cldsToscaModel.setToscaModelName(toscaModelName);
        cldsToscaModel = cldsToscaModel.save(cldsDao, refProp, policyClient, getUserId());
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Parse Tosca model and save success", this.getClass().getName());
        auditLogger.info("Parse Tosca model and save completed");
        return new ResponseEntity<>(cldsToscaModel, HttpStatus.CREATED);
    }

    /**
     * REST service to retrieve all Tosca models from the CLDS database.
     * 
     * @return clds tosca models - list of CLDS tosca models
     */
    public ResponseEntity<List<CldsToscaModel>> getAllToscaModels() {

        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsToscaService: Get All tosca models", getPrincipalName());
        // TODO revisit based on new permissions
        isAuthorized(permissionReadTosca);
        List<CldsToscaModel> cldsToscaModels = Optional.ofNullable(cldsDao.getAllToscaModels()).get();
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get All tosca models success", this.getClass().getName());
        auditLogger.info("Get All tosca models");
        return new ResponseEntity<>(cldsToscaModels, HttpStatus.OK);
    }

    /**
     * REST service that retrieves a CLDS Tosca model by model name from the
     * database.
     * 
     * @param toscaModelName
     *            Path param with tosca model name
     * 
     * @return clds tosca model - CLDS tosca model for a given tosca model name
     */
    public ResponseEntity<CldsToscaModel> getToscaModel(String toscaModelName) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsToscaService: Get tosca models by model name", getPrincipalName());
        // TODO revisit based on new permissions
        isAuthorized(permissionReadTosca);
        List<CldsToscaModel> cldsToscaModels = Optional.ofNullable(cldsDao.getToscaModelByName(toscaModelName)).get();
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get tosca models by model name success", this.getClass().getName());
        auditLogger.info("GET tosca models by model name completed");
        return new ResponseEntity<>(Optional.ofNullable(cldsToscaModels).get().stream().findFirst().get(),
                HttpStatus.OK);
    }

    /**
     * REST service that retrieves a CLDS Tosca model lists for a policy type
     * from the database.
     * 
     * @param policyType
     * @return clds tosca model - CLDS tosca model for a given policy type
     */
    public ResponseEntity<CldsToscaModel> getToscaModelsByPolicyType(String policyType) {
        Date startTime = new Date();
        LoggingUtils.setRequestContext("CldsToscaService: Get tosca models by policyType", getPrincipalName());
        // TODO revisit based on new permissions
        isAuthorized(permissionReadTosca);
        List<CldsToscaModel> cldsToscaModels = Optional.ofNullable(cldsDao.getToscaModelByPolicyType(policyType)).get();
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setResponseContext("0", "Get tosca models by policyType success", this.getClass().getName());
        auditLogger.info("GET tosca models by policyType completed");
        return new ResponseEntity<>(Optional.ofNullable(cldsToscaModels).get().stream().findFirst().get(),
                HttpStatus.OK);
    }

    public ResponseEntity<?> deleteToscaModelById(String toscaModeId) {
        // TODO
        return null;
    }
    
    // Created for the integration test
    public void setLoggingUtil(LoggingUtils utilP) {
        util = utilP;
    }

}
