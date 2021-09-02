/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.tosca.update.execution;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.onap.policy.clamp.clds.tosca.update.execution.cds.ToscaMetadataCdsProcess;
import org.onap.policy.clamp.loop.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to execute a code based on a key found in the metadata section.
 */
@Component
public class ToscaMetadataExecutor {

    private static final Logger logger =
            LoggerFactory.getLogger(ToscaMetadataExecutor.class);

    private Map<String, ToscaMetadataProcess> mapOfProcesses = new HashMap<>();

    /**
     * This method executes the required process specified in processInfo.
     *
     * @param processInfo  A String containing the process to execute, like "cds/param1:value1/param2:value2"
     * @param childObject  The jsonObject
     * @param serviceModel The service model associated to do clamp enrichment
     */
    public void executeTheProcess(String processInfo, JsonObject childObject, Service serviceModel) {
        String[] processParameters = (processInfo + "/ ").split("/");
        /*
         * NOTE: Disabling sonar, because already added / above, hence there is no need
         * to invoke method only conditionally, as sonar suggested.
         */
        String trimStr = processParameters[1].trim();  // NOSONAR
        logger.info("Executing the Tosca clamp process {} with parameters {}",
                   processParameters[0], trimStr);
        mapOfProcesses.get(processParameters[0].trim())
                .executeProcess(trimStr, childObject, serviceModel);
    }

    /**
     * Init method.
     */
    @PostConstruct
    public void init() {
        mapOfProcesses.put("CDS", new ToscaMetadataCdsProcess());
        mapOfProcesses.put("CSAR_RESOURCES", new ToscaMetadataTargetProcess());
    }
}
