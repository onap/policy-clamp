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

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityNotFoundException;

import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.microservice.MicroservicePolicyService;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.onap.clamp.policy.operational.OperationalPolicyService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component
public class LoopService {

    private final LoopsRepository loopsRepository;
    private final MicroservicePolicyService microservicePolicyService;
    private final OperationalPolicyService operationalPolicyService;

    /**
     * Constructor.
     */
    public LoopService(LoopsRepository loopsRepository, MicroservicePolicyService microservicePolicyService,
        OperationalPolicyService operationalPolicyService) {
        this.loopsRepository = loopsRepository;
        this.microservicePolicyService = microservicePolicyService;
        this.operationalPolicyService = operationalPolicyService;
    }

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

    public void updateDcaeDeploymentFields(Loop loop, String deploymentId, String deploymentUrl) {
        loop.setDcaeDeploymentId(deploymentId);
        loop.setDcaeDeploymentStatusUrl(deploymentUrl);
        loopsRepository.save(loop);
    }

    public void updateLoopState(Loop loop, String newState) {
        loop.setLastComputedState(LoopState.valueOf(newState));
        loopsRepository.save(loop);
    }

    Loop updateAndSaveOperationalPolicies(String loopName, List<OperationalPolicy> newOperationalPolicies) {
        Loop loop = findClosedLoopByName(loopName);
        Set<OperationalPolicy> newPolicies = operationalPolicyService.updatePolicies(loop, newOperationalPolicies);
        loop.setOperationalPolicies(newPolicies);
        return loopsRepository.save(loop);
    }

    Loop updateAndSaveMicroservicePolicies(String loopName, List<MicroServicePolicy> newMicroservicePolicies) {
        Loop loop = findClosedLoopByName(loopName);
        Set<MicroServicePolicy> newPolicies = microservicePolicyService.updatePolicies(loop, newMicroservicePolicies);
        loop.setMicroServicePolicies(newPolicies);
        return loopsRepository.save(loop);
    }

    Loop updateAndSaveGlobalPropertiesJson(String loopName, JsonObject newGlobalPropertiesJson) {
        Loop loop = findClosedLoopByName(loopName);
        loop.setGlobalPropertiesJson(newGlobalPropertiesJson);
        return loopsRepository.save(loop);
    }

    MicroServicePolicy updateMicroservicePolicy(String loopName, MicroServicePolicy newMicroservicePolicy) {
        Loop loop = findClosedLoopByName(loopName);
        return microservicePolicyService.getAndUpdateMicroServicePolicy(loop, newMicroservicePolicy);
    }

    private Loop findClosedLoopByName(String loopName) {
        return loopsRepository.findById(loopName)
            .orElseThrow(() -> new EntityNotFoundException("Couldn't find closed loop named: " + loopName));
    }

    public Loop refreshOpPolicyJsonRepresentation(String loopName) {
        Loop loop = findClosedLoopByName(loopName);
        Set<OperationalPolicy> policyList = loop.getOperationalPolicies();
        for (OperationalPolicy policy : policyList) {
            policy.updateJsonRepresentation();
        }
        loop.setOperationalPolicies(policyList);
        return loopsRepository.save(loop);
    }
}
