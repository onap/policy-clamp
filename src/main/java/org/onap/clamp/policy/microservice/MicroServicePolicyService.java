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

package org.onap.clamp.policy.microservice;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.onap.clamp.clds.tosca.update.ToscaConverterWithDictionarySupport;
import org.onap.clamp.loop.Loop;
import org.onap.clamp.policy.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MicroServicePolicyService implements PolicyService<MicroServicePolicy> {

    private final MicroServicePolicyRepository microServiceRepository;

    @Autowired
    public MicroServicePolicyService(MicroServicePolicyRepository microServiceRepository) {
        this.microServiceRepository = microServiceRepository;
    }

    @Override
    public Set<MicroServicePolicy> updatePolicies(Loop loop, List<MicroServicePolicy> newMicroservicePolicies) {
        return newMicroservicePolicies.stream().map(policy -> getAndUpdateMicroServicePolicy(loop, policy))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isExisting(String policyName) {
        return microServiceRepository.existsById(policyName);
    }

    /**
     * Get and update the MicroService policy properties.
     *
     * @param loop   The loop
     * @param policy The new MicroService policy
     * @return The updated MicroService policy
     */
    public MicroServicePolicy getAndUpdateMicroServicePolicy(Loop loop, MicroServicePolicy policy) {
        return microServiceRepository.save(
                microServiceRepository
                        .findById(policy.getName()).map(p -> updateMicroservicePolicyProperties(p, policy, loop))
                        .orElse(new MicroServicePolicy(policy.getName(), policy.getPolicyModel(),
                                policy.getShared(), policy.getJsonRepresentation(), null, policy.getPdpGroup(),
                                policy.getPdpSubgroup())));
    }

    private MicroServicePolicy updateMicroservicePolicyProperties(MicroServicePolicy oldPolicy,
                                                                  MicroServicePolicy newPolicy, Loop loop) {
        oldPolicy.setConfigurationsJson(newPolicy.getConfigurationsJson());
        if (!oldPolicy.getUsedByLoops().contains(loop)) {
            oldPolicy.getUsedByLoops().add(loop);
        }
        oldPolicy.setPdpGroup(newPolicy.getPdpGroup());
        oldPolicy.setPdpSubgroup(newPolicy.getPdpSubgroup());
        return oldPolicy;
    }

    /**
     * Update the MicroService policy deployment related parameters.
     *
     * @param microServicePolicy The micro service policy
     * @param deploymentId       The deployment ID as returned by DCAE
     * @param deploymentUrl      The Deployment URL as returned by DCAE
     */
    public void updateDcaeDeploymentFields(MicroServicePolicy microServicePolicy, String deploymentId,
                                           String deploymentUrl) {
        microServicePolicy.setDcaeDeploymentId(deploymentId);
        microServicePolicy.setDcaeDeploymentStatusUrl(deploymentUrl);
        microServiceRepository.save(microServicePolicy);
    }


    /**
     * Api to refresh the MicroService Policy UI window.
     *
     * @param microServicePolicy The micro Service policy object
     * @param toscaConverter     The tosca converter required to convert the tosca model to json schema
     * @param loop               As a microservice object can belong to multiple loops, we need it here
     */
    public void refreshMicroServicePolicyJsonRepresentation(MicroServicePolicy microServicePolicy,
                                                            ToscaConverterWithDictionarySupport toscaConverter,
                                                            Loop loop) {
        microServicePolicy.updateJsonRepresentation(toscaConverter, loop.getModelService());
        this.microServiceRepository.saveAndFlush(microServicePolicy);

    }
}
