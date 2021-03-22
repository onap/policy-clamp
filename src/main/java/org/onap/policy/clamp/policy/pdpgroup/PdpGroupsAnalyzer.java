/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * This is an utility class to do searching in pdp groups and create json object describing the result.
 */
public class PdpGroupsAnalyzer {

    public static final String ASSIGNED_PDP_GROUPS_INFO = "pdpGroupInfo";
    public static final String SUPPORTED_PDP_GROUPS_INFO = "supportedPdpGroups";

    private final Map<ToscaConceptIdentifier, Map<String, PdpGroup>> pdpGroupsDeploymentPerPolicy =
            new ConcurrentHashMap<>();

    /**
     * Getter of the GroupDeploymentPerPolicy structure.
     *
     * @return The map of policies.
     */
    public Map<ToscaConceptIdentifier, Map<String, PdpGroup>> getPdpGroupsDeploymentPerPolicy() {
        return pdpGroupsDeploymentPerPolicy;
    }

    private static void addInfoToPdpGroupsStructure(ToscaConceptIdentifier toscaId,
                                                    Map<ToscaConceptIdentifier,
                                                            Map<String,
                                                                    PdpGroup>> pdpGroupsDeploymentPerToscaIdentifier,
                                                    PdpGroup pdpGroupSource,
                                                    PdpSubGroup pdpSubGroupSource) {
        Map<String, PdpGroup> pdpGroupsForToscaId = pdpGroupsDeploymentPerToscaIdentifier.get(toscaId);
        if (pdpGroupsForToscaId == null) {
            pdpGroupsForToscaId = new ConcurrentHashMap<>();
            pdpGroupsDeploymentPerToscaIdentifier.put(toscaId, pdpGroupsForToscaId);
        }
        // Copy the subgroup but empty the policies & types
        PdpSubGroup pdpSubGroupCopy = new PdpSubGroup(pdpSubGroupSource);
        //pdpSubGroupCopy.setPolicies(new ArrayList<>());
        //pdpSubGroupCopy.setSupportedPolicyTypes(new ArrayList<>());

        // Copy the group but empty the subgroups list
        PdpGroup pdpGroupCopy = pdpGroupsForToscaId.get(pdpGroupSource.getName());
        if (pdpGroupCopy == null) {
            pdpGroupCopy = new PdpGroup(pdpGroupSource);
            pdpGroupCopy.setPdpSubgroups(new ArrayList<>());
            pdpGroupsForToscaId.put(pdpGroupCopy.getName(), pdpGroupCopy);
        }
        // Add the subgroups to the group copy
        pdpGroupCopy.getPdpSubgroups().add(pdpSubGroupCopy);
    }

    private void analyzePdpGroups(PdpGroups pdpGroups) {
        (pdpGroups.getGroups() != null ? pdpGroups.getGroups() : Collections.<PdpGroup>emptyList()).stream()
                .forEach(group -> {
                    (group.getPdpSubgroups() != null ? group.getPdpSubgroups() : Collections.<PdpSubGroup>emptyList())
                            .stream()
                            .forEach(subGroup -> {
                                (subGroup.getPolicies() != null ? subGroup.getPolicies() :
                                        Collections.<ToscaConceptIdentifier>emptyList()).stream()
                                        .forEach(policy -> {
                                            PdpGroupsAnalyzer.addInfoToPdpGroupsStructure(policy,
                                                    this.pdpGroupsDeploymentPerPolicy,
                                                    group,
                                                    subGroup);
                                        });
                            });
                });
    }

    /**
     * This method retrieves all pdpGroups and subgroups where a specific policy name/version is deployed.
     *
     * @param policyName The policy name that must be used for searching
     * @param version    THe policy version that must be used for searching
     * @return It returns a JsonObject containing each pdpGroup and subgroups associated
     */
    public JsonObject getSupportedPdpGroupsForPolicy(String policyName, String version) {
        JsonObject policyPdpGroups = new JsonObject();
        JsonArray pdpGroupsArray = new JsonArray();
        policyPdpGroups.add(ASSIGNED_PDP_GROUPS_INFO, pdpGroupsArray);
        pdpGroupsArray.add(JsonUtils.GSON.fromJson(JsonUtils.GSON
                        .toJson(this.pdpGroupsDeploymentPerPolicy.get(new ToscaConceptIdentifier(policyName, version))),
                JsonObject.class));
        return pdpGroupsArray.size() != 0 ? policyPdpGroups : null;

    }

    /**
     * Constructor taking he PDPGroups info from the PEF.
     * It then caches the groups per policies and per types.
     *
     * @param pdpGroups The pdpgroup info from the PEF
     */
    public PdpGroupsAnalyzer(PdpGroups pdpGroups) {
        this.analyzePdpGroups(pdpGroups);
    }

    /**
     * Get supported subGroups based on the defined policy type and version for s specific PDPgroup.
     * It returns null if the Group is TERMINATED or if the policytype/version has not been found in the PDPSubgroups.
     *
     * @param pdpGroup   The pdpGroup that must be analyzed
     * @param policyType The policy type
     * @param version    The version
     * @return The supported subGroups list in Json format
     * @see org.onap.policy.models.pdp.concepts.PdpGroup
     * @see org.onap.policy.models.pdp.enums.PdpState
     */
    private static JsonObject getSupportedPdpSubgroupsForModelType(PdpGroup pdpGroup, String policyType,
                                                                   String version) {
        if (PdpState.TERMINATED.equals(pdpGroup.getPdpGroupState())) {
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
        return supportedSubgroups.size() == 0 ? null : supportedPdpGroup;
    }

    /**
     * This method retrieves all supported pdpGroups and subgroups for a specific policy type/version.
     *
     * @param pdpGroups  The PdpGroups object containing all PEF pdp groups info
     * @param policyType The policy type that must be used for searching
     * @param version    THe policy type version that must be used for searching
     * @return It returns a JsonObject containing each pdpGroup and subgroups associated
     */
    public static JsonObject getSupportedPdpGroupsForModelType(PdpGroups pdpGroups, String policyType, String version) {
        JsonObject supportedPdpGroups = new JsonObject();
        JsonArray pdpGroupsArray = new JsonArray();
        supportedPdpGroups.add(SUPPORTED_PDP_GROUPS_INFO, pdpGroupsArray);

        pdpGroups.getGroups().stream().map(pdpGroup -> PdpGroupsAnalyzer.getSupportedPdpSubgroupsForModelType(pdpGroup,
                policyType, version)).filter(Objects::nonNull)
                .forEach(jsonPdpGroup -> pdpGroupsArray.add(jsonPdpGroup));

        return pdpGroupsArray.size() != 0 ? supportedPdpGroups : null;
    }

    /**
     * This method updates each element in the policyModelsList given in argument based on the pdpGroups given.
     *
     * @param policyModelsList The list of Policy Models where each PolicyModel will be updated
     * @param pdpGroups        The PdpGroups containing all PDP group definition
     */
    public static void updatePdpGroupOfPolicyModels(List<PolicyModel> policyModelsList, PdpGroups pdpGroups) {
        policyModelsList.parallelStream().forEach(policyModel -> {
            policyModel.setPolicyPdpGroup(getSupportedPdpGroupsForModelType(pdpGroups, policyModel.getPolicyModelType(),
                    policyModel.getVersion()));
        });
    }
}