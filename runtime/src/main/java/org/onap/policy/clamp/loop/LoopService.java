/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.loop;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityNotFoundException;
import org.onap.policy.clamp.clds.tosca.update.ToscaConverterWithDictionarySupport;
import org.onap.policy.clamp.loop.template.LoopTemplatesService;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.clamp.loop.template.PolicyModelsService;
import org.onap.policy.clamp.policy.microservice.MicroServicePolicy;
import org.onap.policy.clamp.policy.microservice.MicroServicePolicyService;
import org.onap.policy.clamp.policy.operational.OperationalPolicy;
import org.onap.policy.clamp.policy.operational.OperationalPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoopService {

    @Autowired
    private LoopsRepository loopsRepository;

    @Autowired
    private MicroServicePolicyService microservicePolicyService;

    @Autowired
    private OperationalPolicyService operationalPolicyService;

    @Autowired
    private PolicyModelsService policyModelsService;

    @Autowired
    private LoopTemplatesService loopTemplateService;

    @Autowired
    private ToscaConverterWithDictionarySupport toscaConverter;

    Loop saveOrUpdateLoop(Loop loop) {
        return loopsRepository.save(loop);
    }

    List<String> getClosedLoopNames() {
        return loopsRepository.getAllLoopNames();
    }

    public Loop getLoop(String loopName) {
        return loopsRepository.findById(loopName).orElse(null);
    }

    public void deleteLoop(String loopName) {
        loopsRepository.deleteById(loopName);
    }

    /**
     * Creates a Loop Instance from Loop Template Name.
     *
     * @param loopName     Name of the Loop to be created
     * @param templateName Loop Template to used for Loop
     * @return Loop Instance
     */
    public Loop createLoopFromTemplate(String loopName, String templateName) {
        return loopsRepository
                .save(new Loop(loopName, loopTemplateService.getLoopTemplate(templateName), toscaConverter));
    }

    /**
     * This method is used to refresh the DCAE deployment status fields.
     *
     * @param loop          The loop instance to be modified
     * @param deploymentId  The deployment ID as returned by DCAE
     * @param deploymentUrl The Deployment URL as returned by DCAE
     */
    public void updateDcaeDeploymentFields(Loop loop, String deploymentId, String deploymentUrl) {
        loop.setDcaeDeploymentId(deploymentId);
        loop.setDcaeDeploymentStatusUrl(deploymentUrl);
        loopsRepository.saveAndFlush(loop);
    }

    public void updateLoopState(Loop loop, String newState) {
        loop.setLastComputedState(LoopState.valueOf(newState));
        loopsRepository.save(loop);
    }

    /**
     * This method add an operational policy to a loop instance.
     * This creates an operational policy from the policy model info and not the loop element model
     *
     * @param loopName      The loop name
     * @param policyType    The policy model type
     * @param policyVersion The policy model  version
     * @return The loop modified
     */
    Loop addOperationalPolicy(String loopName, String policyType, String policyVersion) {
        var loop = getLoop(loopName);
        var policyModel = policyModelsService.getPolicyModel(policyType, policyVersion);
        Set<OperationalPolicy> opPolicySet = loop.getOperationalPolicies();
        for (OperationalPolicy opPolicy : opPolicySet) {
            if (opPolicy.getPolicyModel().equals(policyModel)) {
                throw new IllegalArgumentException(
                        "This type of Operational Policy is already added to the loop. Please choose another one.");
            }
        }
        if (policyModel == null) {
            return null;
        }
        loop.addOperationalPolicy(
                new OperationalPolicy(loop, loop.getModelService(), policyModel, toscaConverter));
        return loopsRepository.saveAndFlush(loop);
    }

    /**
     * This method remove an operational policy to a loop instance.
     *
     * @param loopName      The loop name
     * @param policyType    The policy model type
     * @param policyVersion The policy model version
     * @return The loop modified
     */
    Loop removeOperationalPolicy(String loopName, String policyType, String policyVersion) {
        var loop = getLoop(loopName);
        var policyModel = policyModelsService.getPolicyModel(policyType, policyVersion);
        if (policyModel == null) {
            return null;
        }
        for (OperationalPolicy opPolicy : loop.getOperationalPolicies()) {
            if (opPolicy.getPolicyModel().getPolicyModelType().equals(policyType)
                    && opPolicy.getPolicyModel().getVersion().equals(policyVersion)) {
                loop.removeOperationalPolicy(opPolicy);
                break;
            }
        }
        return loopsRepository.saveAndFlush(loop);
    }

    Loop updateAndSaveOperationalPolicies(String loopName, List<OperationalPolicy> newOperationalPolicies) {
        var loop = findClosedLoopByName(loopName);
        Set<OperationalPolicy> newPolicies = operationalPolicyService.updatePolicies(loop, newOperationalPolicies);
        loop.setOperationalPolicies(newPolicies);
        return loopsRepository.save(loop);
    }

    Loop updateAndSaveMicroservicePolicies(String loopName, List<MicroServicePolicy> newMicroservicePolicies) {
        var loop = findClosedLoopByName(loopName);
        Set<MicroServicePolicy> newPolicies = microservicePolicyService.updatePolicies(loop, newMicroservicePolicies);
        loop.setMicroServicePolicies(newPolicies);
        return loopsRepository.save(loop);
    }

    Loop updateAndSaveGlobalPropertiesJson(String loopName, JsonObject newGlobalPropertiesJson) {
        var loop = findClosedLoopByName(loopName);
        loop.setGlobalPropertiesJson(newGlobalPropertiesJson);
        return loopsRepository.save(loop);
    }

    MicroServicePolicy updateMicroservicePolicy(String loopName, MicroServicePolicy newMicroservicePolicy) {
        var loop = findClosedLoopByName(loopName);
        return microservicePolicyService.getAndUpdateMicroServicePolicy(loop, newMicroservicePolicy);
    }

    private Loop findClosedLoopByName(String loopName) {
        return loopsRepository.findById(loopName)
                .orElseThrow(() -> new EntityNotFoundException("Couldn't find closed loop named: " + loopName));
    }
}

