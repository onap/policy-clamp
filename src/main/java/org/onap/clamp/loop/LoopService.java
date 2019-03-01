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

import java.util.List;
import java.util.Set;
import javax.persistence.EntityNotFoundException;
import org.onap.clamp.policy.microservice.MicroservicePolicyService;
import org.onap.clamp.policy.operational.OperationalPolicyService;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.springframework.stereotype.Service;

@Service
public class LoopService {

    private final LoopsRepository loopsRepository;
    private final MicroservicePolicyService microservicePolicyService;
    private final OperationalPolicyService operationalPolicyService;

    public LoopService(LoopsRepository loopsRepository,
        MicroservicePolicyService microservicePolicyService,
        OperationalPolicyService operationalPolicyService) {
        this.loopsRepository = loopsRepository;
        this.microservicePolicyService = microservicePolicyService;
        this.operationalPolicyService = operationalPolicyService;
    }

    Loop addNewLoop(Loop loop) {
        return loopsRepository.save(loop);
    }

    List<String> getClosedLoopNames() {
        return loopsRepository.getAllLoopNames();
    }

    Loop getLoop(String loopName){
        return loopsRepository
            .findById(loopName)
            .orElse(null);
    }

    String getClosedLoopModelSVG(String loopName) {
        Loop closedLoopByName = findClosedLoopByName(loopName);
        return closedLoopByName.getSvgRepresentation();
    }

    Loop updateOperationalPolicies(String loopName, List<OperationalPolicy> newOperationalPolicies) {
        Loop loop = findClosedLoopByName(loopName);
        Set<OperationalPolicy> newPolicies = operationalPolicyService
            .updatePolicies(loop, newOperationalPolicies);

        loop.setOperationalPolicies(newPolicies);
        return loopsRepository.save(loop);
    }

    Loop updateMicroservicePolicies(String loopName, List<MicroServicePolicy> newMicroservicePolicies) {
        Loop loop = findClosedLoopByName(loopName);
        Set<MicroServicePolicy> newPolicies = microservicePolicyService
            .updatePolicies(loop, newMicroservicePolicies);

        loop.setMicroServicePolicies(newPolicies);
        return loopsRepository.save(loop);
    }

    private Loop findClosedLoopByName(String loopName) {
        return loopsRepository.findById(loopName)
            .orElseThrow(() -> new EntityNotFoundException("Couldn't find closed loop named: " + loopName));
    }
}
