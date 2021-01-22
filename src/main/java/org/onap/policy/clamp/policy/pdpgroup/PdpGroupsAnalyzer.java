/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.policy.pdpgroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.sdc.toscaparser.api.elements.PolicyType;

/**
 * This is an utility class to do searching in pdp groups.
 */
public class PdpGroupsAnalyzer {
    /**
     * Get supported subGroups based on the defined policy type and version for s specific PDPgroup.
     * It returns null if the Group is not ACTIVE or if the policytype/version has not been found in the PDPSubgroups.
     *
     * @param pdpGroup   The pdpGroup that must be analyzed
     * @param policyType The policy type
     * @param version    The version
     * @return The supported subGroups list in Json format
     * @see org.onap.policy.models.pdp.concepts.PdpGroup
     * @see org.onap.policy.models.pdp.enums.PdpState
     */
    private static JsonObject getSupportedSubgroups(PdpGroup pdpGroup, String policyType, String version) {
        if (!PdpState.ACTIVE.equals(pdpGroup.getPdpGroupState())) {
            return null;
        }
        JsonObject supportedPdpGroup = new JsonObject();
        JsonArray supportedSubgroups = new JsonArray();
        supportedPdpGroup.add(pdpGroup.getName(), supportedSubgroups);
        pdpGroup.getPdpSubgroups().stream().forEach(pdpSubGroup -> {
            if (pdpSubGroup.getSupportedPolicyTypes().stream().anyMatch(policyTypeIdentifier ->
                    policyType.matches(policyTypeIdentifier.getName().replace(".", "\\.").replace("*", ".*"))
                            && version.equals(policyTypeIdentifier.getVersion()))) {
                supportedSubgroups.add(pdpSubGroup.getPdpType());
            }
        });
        if (supportedSubgroups.size() == 0) {
            return null;
        }
        return supportedPdpGroup;
    }

    /**
     * This method updates each element in the policyModelsList given in argument based on the pdpGroups given.
     *
     * @param policyModelsList The list of Policy Models where each PolicyModel will be updated
     * @param pdpGroups        The PdpGroups containing all PDP group definition
     */
    public static void updatePdpGroup(List<PolicyModel> policyModelsList, PdpGroups pdpGroups) {
        policyModelsList.parallelStream().forEach(policyModel -> {
            JsonObject jsonResult = new JsonObject();
            JsonArray supportedPdpGroups = new JsonArray();
            jsonResult.add("supportedPdpGroups", supportedPdpGroups);
            policyModel.setPolicyPdpGroup(jsonResult);
            pdpGroups.getGroups().stream().map(pdpGroup -> PdpGroupsAnalyzer.getSupportedSubgroups(pdpGroup,
                    policyModel.getPolicyModelType(), policyModel.getVersion())).filter(Objects::nonNull)
                    .forEach(jsonPdpGroup -> supportedPdpGroups.add(jsonPdpGroup));
            if (supportedPdpGroups.size() == 0) {
                policyModel.setPolicyPdpGroup(null);
            }
        });
    }
}