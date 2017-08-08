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
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsEvent;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * Create CLDS Event.
 */
public class CldsEventDelegate implements JavaDelegate {

    protected static final EELFLogger logger        = EELFManager.getInstance().getLogger(CldsEventDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    @Autowired
    private CldsDao                 cldsDao;

    /**
     * Insert event using process variables.
     *
     * @param execution
     */
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String controlName = (String) execution.getVariable("controlName");
        String actionCd = (String) execution.getVariable("actionCd");
        String actionStateCd = (String) execution.getVariable("actionStateCd");
        // Flag indicate whether it is triggered by Validation Test button from UI
        boolean isTest = (boolean) execution.getVariable("isTest");
        boolean isInsertTestEvent = (boolean) execution.getVariable("isInsertTestEvent");
        String userid = (String) execution.getVariable("userid");

        // do not insert events for test actions unless flag set to insert them
        if (!isTest || isInsertTestEvent) {
            // won't really have userid here...
            CldsEvent.insEvent(cldsDao, controlName, userid, actionCd, actionStateCd, execution.getProcessInstanceId());
        }
    }

}
