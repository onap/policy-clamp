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

package org.onap.clamp.clds.client;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import org.apache.camel.Exchange;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Create CLDS Event.
 */
@Component
public class CldsEventDelegate {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsEventDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    @Autowired
    private CldsDao cldsDao;

    /**
     * Insert event using process variables.
     *
     * @param camelExchange
     *        The Camel Exchange object containing the properties
     * @param actionState
     *        The action state that is used instead of the one in exchange property
     */

    public void addEvent(Exchange camelExchange, String actionState) {
        String controlName = (String) camelExchange.getProperty("controlName");
        String actionCd = (String) camelExchange.getProperty("actionCd");
        String actionStateCd = (actionState != null) ? actionState : CldsEvent.ACTION_STATE_COMPLETED;
        actionStateCd = ((String) camelExchange.getProperty("actionStateCd")) != null
            ? ((String) camelExchange.getProperty("actionStateCd"))
            : actionStateCd;
        // Flag indicate whether it is triggered by Validation Test button from
        // UI
        boolean isTest = (boolean) camelExchange.getProperty("isTest");
        boolean isInsertTestEvent = (boolean) camelExchange.getProperty("isInsertTestEvent");
        String userid = (String) camelExchange.getProperty("userid");
        // do not insert events for test actions unless flag set to insert them
        if (!isTest || isInsertTestEvent) {
            // won't really have userid here...
            CldsEvent.insEvent(cldsDao, controlName, userid, actionCd, actionStateCd, camelExchange.getExchangeId());
        }
    }
}
