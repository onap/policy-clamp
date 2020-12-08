/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.tosca.update.execution.target;

import com.google.gson.JsonObject;
import org.onap.clamp.clds.tosca.update.execution.ToscaMetadataProcess;
import org.onap.clamp.loop.service.Service;
import org.onap.clamp.policy.operational.OperationalPolicyRepresentationBuilder;

/**
 * This class is there to add the JsonObject for CDS in the json Schema according to what is found in the Tosca model.
 */
public class ToscaMetadataTargetProcess extends ToscaMetadataProcess {

    @Override
    public void executeProcess(String parameters, JsonObject childObject, Service serviceModel) {
        childObject.add("anyOf", OperationalPolicyRepresentationBuilder.createAnyOfArray(serviceModel, false));
    }
}
