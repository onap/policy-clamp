/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.flow.log;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import javax.servlet.http.HttpServletRequest;

import org.apache.camel.Exchange;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.clamp.clds.util.OnapLogConstants;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * The Flow log operations.
 */
@Component
public class FlowLogOperation {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(FlowLogOperation.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getMetricsLogger();
    private LoggingUtils util = new LoggingUtils(logger);

    @Autowired
    private HttpServletRequest request;

    /**
     * Generate the entry log.
     *
     * @param serviceDesc
     *        The service description the loop name
     */
    public void startLog(Exchange exchange, String serviceDesc) {
        util.entering(request, serviceDesc);
        exchange.setProperty(OnapLogConstants.Headers.REQUEST_ID, util.getProperties(OnapLogConstants.Mdcs.REQUEST_ID));
        exchange.setProperty(OnapLogConstants.Headers.INVOCATION_ID,
            util.getProperties(OnapLogConstants.Mdcs.INVOCATION_ID));
        exchange.setProperty(OnapLogConstants.Headers.PARTNER_NAME,
            util.getProperties(OnapLogConstants.Mdcs.PARTNER_NAME));
    }

    /**
     * Generate the exiting log.
     */
    public void endLog() {
        util.exiting(HttpStatus.OK.toString(), "Successful", Level.INFO, OnapLogConstants.ResponseStatus.COMPLETED);
    }

    /**
     * Generate the error exiting log.
     */
    public void errorLog() {
        util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Failed", Level.INFO,
            OnapLogConstants.ResponseStatus.ERROR);
    }

    /**
     * Generate the error exiting log.
     */
    public void httpErrorLog() {

    }

    /**
     * Generate the invoke log.
     */
    public void invokeLog(String targetEntity, String targetServiceName) {
        util.invoke(targetEntity, targetServiceName);
    }

    /**
     * Generate the invoke return marker.
     */
    public void invokeReturnLog() {
        util.invokeReturn();
    }
}
