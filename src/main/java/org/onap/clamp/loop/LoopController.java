/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
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

package org.onap.clamp.loop;

import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;


@Controller
public class LoopController {

    private final LoopService loopService;
    private static final Type OPERATIONAL_POLICY_TYPE = new TypeToken<List<OperationalPolicy>>() {
    }.getType();
    private static final Type MICROSERVICE_POLICY_TYPE = new TypeToken<List<MicroServicePolicy>>() {
    }.getType();

    @Autowired
    public LoopController(LoopService loopService) {
        this.loopService = loopService;
    }

    public List<String> getLoopNames() {
        return loopService.getClosedLoopNames();
    }

    public Loop getLoop(String loopName) {
        return loopService.getLoop(loopName);
    }

    public Loop updateOperationalPolicies(String loopName, JsonArray operationalPoliciesJson) {
        List<OperationalPolicy> operationalPolicies = JsonUtils.GSON
            .fromJson(operationalPoliciesJson, OPERATIONAL_POLICY_TYPE);
        return loopService.updateOperationalPolicies(loopName, operationalPolicies);
    }

    public Loop updateMicroservicePolicies(String loopName, JsonArray microServicePoliciesJson) {
        List<MicroServicePolicy> microservicePolicies = JsonUtils.GSON
            .fromJson(microServicePoliciesJson, MICROSERVICE_POLICY_TYPE);
        return loopService.updateMicroservicePolicies(loopName, microservicePolicies);
    }
}
