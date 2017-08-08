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

package org.onap.clamp.clds.client;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.onap.clamp.clds.client.req.DcaeReq;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * Send control loop model to dcae proxy.
 */
public class DcaeReqDelegate implements JavaDelegate {
    protected static final EELFLogger logger        = EELFManager.getInstance().getLogger(DcaeReqDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    @Autowired
    private RefProp                 refProp;

    @Value("${org.onap.clamp.config.dcae.url:http://localhost:9000/closedloop-dcae-services}")
    private String                  cldsDcaeUrl;

    /**
     * Perform activity. Send to dcae proxy.
     *
     * @param execution
     */
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        ModelProperties prop = ModelProperties.create(execution);
        String dcaeReq = DcaeReq.format(refProp, prop);
        if (dcaeReq != null) {
            execution.setVariable("dcaeReq", dcaeReq.getBytes());
        }
        execution.setVariable("dcaeUrl", cldsDcaeUrl + "/" + prop.getControlName());
    }
}
