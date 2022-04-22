/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018, 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.service;

import java.util.Date;
import org.onap.policy.clamp.clds.model.CldsHealthCheck;
import org.onap.policy.clamp.clds.util.LoggingUtils;
import org.onap.policy.clamp.clds.util.OnapLogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Service to retrieve the Health Check of the clds application.
 *
 */
@Component
public class CldsHealthcheckService {

    protected static final Logger logger = LoggerFactory.getLogger(CldsHealthcheckService.class);

    /**
     * REST service that retrieves clds healthcheck information.
     *
     * @return CldsHealthCheck class containing healthcheck info
     */
    public CldsHealthCheck gethealthcheck() {
        var cldsHealthCheck = new CldsHealthCheck();
        var startTime = new Date();
        var util = new LoggingUtils(logger);
        LoggingUtils.setRequestContext("CldsService: GET healthcheck", "Clamp-Health-Check");
        LoggingUtils.setTimeContext(startTime, new Date());
        try {
            cldsHealthCheck.setHealthCheckComponent("CLDS-APP");
            cldsHealthCheck.setHealthCheckStatus("UP");
            cldsHealthCheck.setDescription("OK");
            LoggingUtils.setResponseContext("0", "Get healthcheck success",
                this.getClass().getName());
            util.exiting(HttpStatus.OK.value(), "Healthcheck success", Level.INFO,
                OnapLogConstants.ResponseStatus.COMPLETE);
        } catch (Exception e) {
            logger.error("CLAMP application Heath check failed", e);
            LoggingUtils.setResponseContext("999", "Get healthcheck failed",
                this.getClass().getName());
            cldsHealthCheck.setHealthCheckComponent("CLDS-APP");
            cldsHealthCheck.setHealthCheckStatus("DOWN");
            cldsHealthCheck.setDescription("NOT-OK");
            util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Healthcheck failed", Level.INFO,
                OnapLogConstants.ResponseStatus.ERROR);
        }
        return cldsHealthCheck;
    }
}
