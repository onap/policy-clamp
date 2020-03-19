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
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import org.onap.clamp.clds.tosca.update.ToscaConverterWithDictionarySupport;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.microservice.MicroServicePolicyService;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.onap.clamp.policy.operational.OperationalPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class LoopController {

    private final LoopService loopService;

    private final ToscaConverterWithDictionarySupport toscaConverter;

    private final OperationalPolicyService operationalPolicyService;

    private final MicroServicePolicyService microServicePolicyService;

    private static final Type OPERATIONAL_POLICY_TYPE = new TypeToken<List<OperationalPolicy>>() {
    }.getType();

    private static final Type MICROSERVICE_POLICY_TYPE = new TypeToken<List<MicroServicePolicy>>() {
    }.getType();


    /**
     * Constructor.
     *
     * @param loopService               loopService
     * @param operationalPolicyService  operationalPolicyService
     * @param microServicePolicyService microServicePolicyService
     * @param toscaConverter            toscaConverter
     */
    @Autowired
    public LoopController(LoopService loopService, OperationalPolicyService operationalPolicyService,
                          MicroServicePolicyService microServicePolicyService,
                          ToscaConverterWithDictionarySupport toscaConverter) {
        this.loopService = loopService;
        this.toscaConverter = toscaConverter;
        this.operationalPolicyService = operationalPolicyService;
        this.microServicePolicyService = microServicePolicyService;
    }

    public Loop createLoop(String loopName, String templateName) {
        return loopService.createLoopFromTemplate(loopName, templateName);
    }

    public List<String> getLoopNames() {
        return loopService.getClosedLoopNames();
    }

    public Loop getLoop(String loopName) {
        return loopService.getLoop(loopName);
    }

    /**
     * Update the Operational Policy properties.
     *
     * @param loopName                The loop name
     * @param operationalPoliciesJson The new Operational Policy properties
     * @return The updated loop
     */
    public Loop updateOperationalPolicies(String loopName, JsonArray operationalPoliciesJson) {
        List<OperationalPolicy> operationalPolicies = JsonUtils.GSON_JPA_MODEL.fromJson(operationalPoliciesJson,
                OPERATIONAL_POLICY_TYPE);
        return loopService.updateAndSaveOperationalPolicies(loopName, operationalPolicies);
    }

    /**
     * Update the whole array of MicroService policies properties.
     *
     * @param loopName                 The loop name
     * @param microServicePoliciesJson The array of all MicroService policies
     *                                 properties
     * @return The updated loop
     */
    public Loop updateMicroservicePolicies(String loopName, JsonArray microServicePoliciesJson) {
        List<MicroServicePolicy> microservicePolicies = JsonUtils.GSON_JPA_MODEL.fromJson(microServicePoliciesJson,
                MICROSERVICE_POLICY_TYPE);
        return loopService.updateAndSaveMicroservicePolicies(loopName, microservicePolicies);
    }

    /**
     * Update the global properties.
     *
     * @param loopName         The loop name
     * @param globalProperties The updated global properties
     * @return The updated loop
     */
    public Loop updateGlobalPropertiesJson(String loopName, JsonObject globalProperties) {
        return loopService.updateAndSaveGlobalPropertiesJson(loopName, globalProperties);
    }

    /**
     * This method add an operational policy to a loop instance.
     *
     * @param loopName      The loop name
     * @param policyType    The policy model type
     * @param policyVersion The policy model  version
     * @return The loop modified
     */
    public Loop addOperationalPolicy(String loopName, String policyType, String policyVersion) throws IOException {
        return loopService.addOperationalPolicy(loopName, policyType, policyVersion);
    }

    /**
     * This method remove an operational policy to a loop instance.
     *
     * @param loopName      The loop name
     * @param policyType    The policy model type
     * @param policyVersion The policy model  version
     * @return The loop modified
     */
    public Loop removeOperationalPolicy(String loopName, String policyType, String policyVersion) {
        return loopService.removeOperationalPolicy(loopName, policyType, policyVersion);
    }

    /**
     * This method deletes the loop.
     *
     * @param loopName The loop Name
     */
    public void deleteLoop(String loopName) {
        loopService.deleteLoop(loopName);
    }

    /**
     * Update one MicroService policy properties.
     *
     * @param loopName              The loop name
     * @param newMicroservicePolicy The new MicroService policy properties
     * @return The updated MicroService policy
     */
    public MicroServicePolicy updateMicroservicePolicy(String loopName, MicroServicePolicy newMicroservicePolicy) {
        return loopService.updateMicroservicePolicy(loopName, newMicroservicePolicy);
    }

    /**
     * Get the SVG representation of the loop.
     *
     * @param loopName The loop name
     * @return The SVG representation
     */
    public String getSvgRepresentation(String loopName) {
        Loop loop = loopService.getLoop(loopName);
        return loop != null ? loop.getSvgRepresentation() : null;
    }

    /**
     * Refresh the Operational Policy Json representation of the loop.
     *
     * @param loop                  The loop
     * @param operationalPolicyName The operational policy name that needs a refresh
     * @return The loop object
     */
    public Loop refreshOperationalPolicyJsonRepresentation(Loop loop, String operationalPolicyName) {
        for (OperationalPolicy operationalPolicy : loop.getOperationalPolicies()) {
            if (operationalPolicy.getName().equals(operationalPolicyName)) {
                this.operationalPolicyService
                        .refreshOperationalPolicyJsonRepresentation(operationalPolicy, toscaConverter);
            }
        }
        return loop;
    }

    /**
     * Refresh the Config Policy Json representation of the loop.
     *
     * @param loop                   The loop
     * @param microServicePolicyName The microservice policy name that needs a refresh
     * @return The loop object
     */
    public Loop refreshMicroServicePolicyJsonRepresentation(Loop loop, String microServicePolicyName) {
        for (MicroServicePolicy microServicePolicy : loop.getMicroServicePolicies()) {
            if (microServicePolicy.getName().equals(microServicePolicyName)) {
                this.microServicePolicyService
                        .refreshMicroServicePolicyJsonRepresentation(microServicePolicy, toscaConverter, loop);
            }
        }
        return loop;
    }
}
