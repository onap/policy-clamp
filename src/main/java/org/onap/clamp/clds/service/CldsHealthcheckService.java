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
 */
package org.onap.clamp.clds.service;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.util.Date;

import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsHealthCheck;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Service to retrieve the Health Check of the clds application.
 *
 */
@Component
public class CldsHealthcheckService {

    @Autowired
    private CldsDao cldsDao;

    protected static final EELFLogger logger          = EELFManager.getInstance().getLogger(CldsHealthcheckService.class);

    /**
     * REST service that retrieves clds healthcheck information.
     *
     * @return CldsHealthCheck class containing healthcheck info
     */
    public ResponseEntity<CldsHealthCheck> gethealthcheck() {
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
            logger.error("CLAMP application Heath check failed", e);
            LoggingUtils.setResponseContext("999", "Get healthcheck failed", this.getClass().getName());
            cldsHealthCheck.setHealthCheckComponent("CLDS-APP");
            cldsHealthCheck.setHealthCheckStatus("DOWN");
            cldsHealthCheck.setDescription("NOT-OK");
        }
        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        if(healthcheckFailed) {
            return new ResponseEntity<>(cldsHealthCheck, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            return new ResponseEntity<>(cldsHealthCheck, HttpStatus.OK);
        }
    }
}