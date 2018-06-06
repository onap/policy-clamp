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

package org.onap.clamp.clds.client;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.onap.clamp.clds.dao.CldsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Delete model.
 */
@Component
public class ModelDeleteDelegate {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(ModelDeleteDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    @Autowired
    private CldsDao cldsDao;

    /**
     * Insert event using process variables.
     *
     * @param camelExchange
     *            The Camel Exchange object containing the properties
     */
    @Handler
    public void execute(Exchange camelExchange) {
        String modelName = (String) camelExchange.getProperty("modelName");
        cldsDao.deleteModel(modelName);
    }
}
