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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonObject;
import org.onap.policy.clamp.loop.service.Service;
import org.onap.policy.clamp.policy.operational.OperationalPolicyRepresentationBuilder;

/**
 * This class is there to add the JsonObject for CDS in the json Schema according to what is found in the Tosca model.
 */
public class ToscaMetadataTargetProcess extends ToscaMetadataProcess {


    private static final EELFLogger logger =
            EELFManager.getInstance().getLogger(ToscaMetadataTargetProcess.class);

    @Override
    public void executeProcess(String parameters, JsonObject childObject, Service serviceModel) {
        if (serviceModel == null) {
            logger.info("serviceModel is null, therefore the ToscaMetadataTargetProcess is skipped");
            return;
        }
        childObject.add("anyOf", OperationalPolicyRepresentationBuilder.createAnyOfArray(serviceModel, false));
    }
}
