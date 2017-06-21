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

package org.onap.clamp.clds.service.rs;

import com.att.ajsc.common.AjscService;
import org.onap.clamp.clds.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to invoke example Camunda process.
 * <p>
 * Try testing by using:
 * http://[hostname]:[serverPort]/services/log/log-message/your-message-here
 */
@AjscService
public class JaxrsLogServiceImpl implements JaxrsLogService {

    @Autowired
    private LogService logService;

    /**
     * REST service that executes example camunda process to log input message.
     *
     * @param logMessageText
     * @return output from service - comment on what was done
     */
    public String logMessage(String logMessageText, String javamail, String springmail, String commonsmail) {
        return logService.logMessage(logMessageText, javamail, springmail, commonsmail);
    }

    /**
     * REST service that executes example camunda process to log input message.
     *
     * @return output from service - comment on what was done
     */
    public String postLogMessage(String histEventList) {
        return logService.postLogMessage(histEventList);
    }

    /**
     * REST service that executes example camunda process to log input message.
     *
     * @param startTime
     * @param endTime
     * @param serviceName
     * @return output from service - comment on what was done
     */
    public String createLogMessage(String startTime, String endTime, String serviceName) {
        return logService.createLogMessage(startTime, endTime, serviceName);
    }

    /**
     * REST service that executes example camunda process to log input message.
     *
     * @param procInstId
     * @param histEventList
     * @return output from service - comment on what was done
     */
    public String createLogMessageUsingHistory(String procInstId, String histEventList) {
        return logService.createLogMessageUsingHistory(procInstId, histEventList);
    }

    /**
     * REST service that executes example camunda process to log input message.
     *
     * @param procInstId
     * @return output from service - comment on what was done
     */
    public String CreateHistLog(String procInstId) {
        return logService.CreateHistLog(procInstId);
    }

}
