/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.participant.policy.client;

import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.ParticipantPolicyParameters;
import org.onap.policy.models.pdp.concepts.DeploymentGroup;
import org.onap.policy.models.pdp.concepts.DeploymentGroups;
import org.onap.policy.models.pdp.concepts.DeploymentSubGroup;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.stereotype.Component;

@Component
public class PolicyPapHttpClient extends AbstractHttpClient {

    private static final String PAP_URI = "/policy/pap/v1/";
    private final String pdpGroup;
    private final String pdpType;

    /**
     * Constructor.
     *
     * @param parameters the policy participant parameters
     */
    public PolicyPapHttpClient(ParticipantPolicyParameters parameters) {
        super(parameters.getPolicyPapParameters());
        this.pdpGroup = parameters.getPdpGroup();
        this.pdpType = parameters.getPdpType();
    }

    /**
     * Deploy or undeploy Policies.
     *
     * @param policyName the name of the policy to be deployed/undeployed
     * @param policyVersion the version of the policy to be deployed/undeployed
     * @param action the action to deploy/undeploy policy
     * @return Response
     */
    public Response handlePolicyDeployOrUndeploy(final String policyName, final String policyVersion,
                                                 final DeploymentSubGroup.Action action) {

        List<ToscaConceptIdentifier> policies = new LinkedList<ToscaConceptIdentifier>();
        policies.add(new ToscaConceptIdentifier(policyName, policyVersion));

        DeploymentSubGroup subGroup = new DeploymentSubGroup();
        subGroup.setPolicies(policies);
        subGroup.setPdpType(pdpType);
        subGroup.setAction(action);

        DeploymentGroup group = new DeploymentGroup();
        List<DeploymentSubGroup> subGroups = new LinkedList<DeploymentSubGroup>();
        subGroups.add(subGroup);
        group.setDeploymentSubgroups(subGroups);
        group.setName(pdpGroup);

        DeploymentGroups groups = new DeploymentGroups();
        List<DeploymentGroup> groupsArr = new LinkedList<DeploymentGroup>();
        groupsArr.add(group);
        groups.setGroups(groupsArr);

        return executePost(PAP_URI + "pdps/deployments/batch", Entity.entity(groups, MediaType.APPLICATION_JSON));
    }
}
