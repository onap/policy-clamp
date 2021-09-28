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

package org.onap.policy.clamp.controlloop.runtime.util;

import java.util.List;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.provider.PolicyModelsProviderFactory;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to hold/create all parameters for test cases.
 *
 */
public class CommonTestData {
    private static final Coder CODER = new StandardCoder();

    /**
     * Gets the standard Control Loop parameters.
     *
     * @param dbName the database name
     * @return the standard Control Loop parameters
     * @throws ControlLoopRuntimeException on errors reading the control loop parameters
     */
    public static ClRuntimeParameterGroup geParameterGroup(final String dbName) {
        try {
            return CODER.convert(getParameterGroupAsString(dbName), ClRuntimeParameterGroup.class);

        } catch (CoderException e) {
            throw new ControlLoopRuntimeException(Status.NOT_ACCEPTABLE, "cannot read Control Loop parameters", e);
        }
    }

    /**
     * Gets the standard Control Loop parameters, as a String.
     *
     * @param dbName the database name
     * @return the standard Control Loop parameters as string
     */
    public static String getParameterGroupAsString(final String dbName) {
        return ResourceUtils.getResourceAsString("src/test/resources/parameters/TestParameters.json")
                .replace("${dbName}", "jdbc:h2:mem:" + dbName);
    }

    /**
     * Create a new PolicyModelsProvider.
     *
     * @param databaseProviderParameters the database Provider Parameters
     * @return a new PolicyModelsProvider
     */
    public static PolicyModelsProvider getPolicyModelsProvider(
            PolicyModelsProviderParameters databaseProviderParameters) {
        try {
            return new PolicyModelsProviderFactory().createPolicyModelsProvider(databaseProviderParameters);
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }
    }

    /**
     * Create a List of Participants.
     *
     * @return a List of Participants
     */
    public static List<Participant> createParticipants() {
        var participant1 = createParticipant(
                new ToscaConceptIdentifier("org.onap.dcae.controlloop.DCAEMicroserviceControlLoopParticipant", "2.3.4"),
                new ToscaConceptIdentifier("org.onap.dcae.controlloop.DCAEMicroserviceControlLoopParticipant",
                        "2.3.4"));
        var participant2 = createParticipant(
                new ToscaConceptIdentifier("org.onap.policy.controlloop.PolicyControlLoopParticipant", "2.3.1"),
                new ToscaConceptIdentifier("org.onap.policy.controlloop.PolicyControlLoopParticipant", "2.3.1"));
        var participant3 = createParticipant(
                new ToscaConceptIdentifier("org.onap.ccsdk.cds.controlloop.CdsControlLoopParticipant", "2.2.1"),
                new ToscaConceptIdentifier("org.onap.ccsdk.cds.controlloop.CdsControlLoopParticipant", "2.2.1"));
        return List.of(participant1, participant2, participant3);
    }

    /**
     * Create a new Participant.
     *
     * @param participantType the participant Type
     * @param participantId the participant id
     * @return a new Participant
     */
    public static Participant createParticipant(ToscaConceptIdentifier participantType,
            ToscaConceptIdentifier participantId) {
        var participant = new Participant();
        participant.setDefinition(participantId);
        participant.setParticipantType(participantType);
        participant.setName(participantId.getName());
        participant.setVersion(participantId.getVersion());
        return participant;
    }
}
