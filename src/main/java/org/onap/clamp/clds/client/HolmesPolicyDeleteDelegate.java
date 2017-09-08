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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.onap.clamp.clds.model.prop.Holmes;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Delete Holmes Policy via policy api.
 */
public class HolmesPolicyDeleteDelegate implements JavaDelegate {
    protected static final EELFLogger logger        = EELFManager.getInstance()
            .getLogger(HolmesPolicyDeleteDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    @Autowired
    private PolicyClient              policyClient;

    /**
     * Perform activity. Delete Holmes Policy via policy api.
     *
     * @param execution
     */
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        ModelProperties prop = ModelProperties.create(execution);
        Holmes holmes = prop.getType(Holmes.class);
        if (holmes.isFound()) {
            prop.setCurrentModelElementId(holmes.getId());

            policyClient.deleteBasePolicy(prop);
        }
    }

}
