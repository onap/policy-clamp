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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Arrays;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.models.pdp.concepts.DeploymentGroup;
import org.onap.policy.models.pdp.concepts.DeploymentGroups;
import org.onap.policy.models.pdp.concepts.DeploymentSubGroup;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * This is an utility class that build the PDP group policy payload.
 * This is used when policies have to be deployed to PDP group/subgroups on the Policy Engine.
 * Currently it does not group the queries per pdpgroup/subgroups/action.
 * This is currently NOT thread safe, do not use parallel streams to update the structure.
 */
public class PdpGroupPayload {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(PdpGroupPayload.class);

    /**
     * The default node that will contain the actions array.
     */
    public static final String PDP_ACTIONS = "PdpActions";

    private final DeploymentGroups deploymentGroups = new DeploymentGroups();

    /**
     * Default constructor.
     */
    public PdpGroupPayload() {
        deploymentGroups.setGroups(new ArrayList<>());
    }

    /**
     * Constructor that takes a list of actions in input.
     *
     * @param listOfPdpActions The list of actions that needs to be done.
     *                            e.g: {"Pdpactions":["DELETE/PdpGroup1/PdpSubGroup1/PolicyName1/1.0.0",....]}
     * @throws PdpGroupPayloadException in case of issues to read the listOfActions
     */
    public PdpGroupPayload(final JsonElement listOfPdpActions) throws PdpGroupPayloadException {
        this();
        this.readListOfActions(listOfPdpActions);
    }

    /**
     * This method converts the list of actions directly to the pdp payload query as String.
     *
     * @param listOfPdpActions The list of actions that needs to be done.
     *                            e.g: {"Pdpactions":["DELETE/PdpGroup1/PdpSubGroup1/PolicyName1/1.0.0",....]}
     * @return The string containing the PDP payload that can be sent directly
     * @throws PdpGroupPayloadException in case of issues to read the listOfActions
     */
    public static String generatePdpGroupPayloadFromList(final JsonElement listOfPdpActions)
            throws PdpGroupPayloadException {
        return new PdpGroupPayload(listOfPdpActions).generatePdpGroupPayload();
    }


    private void readListOfActions(final JsonElement listOfPdpActions) throws PdpGroupPayloadException {
        for (JsonElement action : listOfPdpActions.getAsJsonObject().getAsJsonArray(PDP_ACTIONS)) {
            String[] opParams = action.getAsString().split("/");
            if (opParams.length == 5) {
                this.updatePdpGroupMap(opParams[1], opParams[2], opParams[3], opParams[4], opParams[0]);
            } else {
                logger.error("One PDP push command does not contain the right number of arguments: " + action);
                throw new PdpGroupPayloadException(
                        "One PDP push command does not contain the right number of arguments: " + action);
            }
        }
    }

    /**
     * This method updates the pdpGroupMap structure for a specific policy/version/pdpdGroup/PdpSubGroup.
     *
     * @param pdpGroup      The pdp Group in String
     * @param pdpSubGroup   The pdp Sub Group in String
     * @param policyName    The policy name
     * @param policyVersion The policy Version
     * @param action        DELETE or POST
     */
    public void updatePdpGroupMap(String pdpGroup,
                                  String pdpSubGroup,
                                  String policyName,
                                  String policyVersion, String action) {
        // create subgroup
        DeploymentSubGroup newSubGroup = new DeploymentSubGroup();
        newSubGroup.setPdpType(pdpSubGroup);
        newSubGroup.setAction(DeploymentSubGroup.Action.valueOf(action));
        newSubGroup.setPolicies(Arrays.asList(new ToscaConceptIdentifier(policyName, policyVersion)));
        // Then the group
        DeploymentGroup newGroup = new DeploymentGroup();
        newGroup.setName(pdpGroup);
        newGroup.setDeploymentSubgroups(Arrays.asList(newSubGroup));
        // Add to deployment Groups structure
        this.deploymentGroups.getGroups().add(newGroup);
    }

    /**
     * This method generates the Payload in Json from the pdp Group structure containing the policies/versions
     * that must be sent to the policy framework.
     *
     * @return The Json that can be sent to policy framework as String
     */
    public String generatePdpGroupPayload() {
        String payload = JsonUtils.GSON.toJson(this.deploymentGroups);
        logger.info("PdpGroup policy payload: " + payload);
        return payload;
    }
}
