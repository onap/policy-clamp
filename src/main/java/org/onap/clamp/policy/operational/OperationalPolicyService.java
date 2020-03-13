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

package org.onap.clamp.policy.operational;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.onap.clamp.loop.Loop;
import org.onap.clamp.loop.template.PolicyModelsRepository;
import org.onap.clamp.policy.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OperationalPolicyService implements PolicyService<OperationalPolicy> {

    private final OperationalPolicyRepository operationalPolicyRepository;

    private final PolicyModelsRepository policyModelsRepository;

    @Autowired
    public OperationalPolicyService(OperationalPolicyRepository repository,
                                    PolicyModelsRepository policyModelsRepository) {
        this.operationalPolicyRepository = repository;
        this.policyModelsRepository = policyModelsRepository;
    }

    @Override
    public Set<OperationalPolicy> updatePolicies(Loop loop, List<OperationalPolicy> operationalPolicies) {
        return operationalPolicies
                .parallelStream()
                .map(policy ->
                        operationalPolicyRepository
                                .findById(policy.getName())
                                .map(p -> setConfiguration(p, policy))
                                .orElse(initializeMissingFields(loop,policy)))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isExisting(String policyName) {
        return operationalPolicyRepository.existsById(policyName);
    }

    private OperationalPolicy initializeMissingFields(Loop loop, OperationalPolicy policy) {
        policy.setLoop(loop);
        return policy;
    }

    private OperationalPolicy setConfiguration(OperationalPolicy policy, OperationalPolicy newPolicy) {
        policy.setConfigurationsJson(newPolicy.getConfigurationsJson());
        policy.setPdpGroup(newPolicy.getPdpGroup());
        policy.setPdpSubgroup(newPolicy.getPdpSubgroup());
        return policy;
    }
}
