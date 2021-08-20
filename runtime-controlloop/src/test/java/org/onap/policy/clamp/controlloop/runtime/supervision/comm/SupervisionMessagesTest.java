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

package org.onap.policy.clamp.controlloop.runtime.supervision.comm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ClElementStatisticsProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantStatisticsProvider;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.clamp.controlloop.runtime.util.rest.CommonRestController;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.YamlJsonTranslator;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class SupervisionMessagesTest extends CommonRestController {

    private static final String TOSCA_SERVICE_TEMPLATE_YAML =
            "src/test/resources/rest/servicetemplates/pmsh_multiple_cl_tosca.yaml";
    private static final Object lockit = new Object();
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";
    private static SupervisionHandler supervisionHandler;
    private static CommissioningProvider commissioningProvider;
    private static ControlLoopProvider clProvider;
    private static PolicyModelsProvider modelsProvider;
    private static final YamlJsonTranslator yamlTranslator = new YamlJsonTranslator();
    private static final String TOSCA_TEMPLATE_YAML =
            "src/test/resources/rest/servicetemplates/pmsh_multiple_cl_tosca.yaml";
    private static final String CONTROL_LOOP_ELEMENT = "org.onap.policy.clamp.controlloop.ControlLoopElement";
    private static final Coder CODER = new StandardCoder();

    /**
     * setup Db Provider Parameters.
     *
     * @throws PfModelException if an error occurs
     */
    @BeforeAll
    public static void setupDbProviderParameters() throws PfModelException {
        ClRuntimeParameterGroup controlLoopParameters = CommonTestData.geParameterGroup("instantproviderdb");

        modelsProvider = CommonTestData.getPolicyModelsProvider(controlLoopParameters.getDatabaseProviderParameters());
        clProvider = new ControlLoopProvider(controlLoopParameters.getDatabaseProviderParameters());
        var participantStatisticsProvider =
                new ParticipantStatisticsProvider(controlLoopParameters.getDatabaseProviderParameters());
        var clElementStatisticsProvider =
                new ClElementStatisticsProvider(controlLoopParameters.getDatabaseProviderParameters());
        commissioningProvider = new CommissioningProvider(modelsProvider, clProvider);
        var monitoringProvider =
                new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);
        var participantProvider = new ParticipantProvider(controlLoopParameters.getDatabaseProviderParameters());
        var controlLoopUpdatePublisher = Mockito.mock(ControlLoopUpdatePublisher.class);
        var controlLoopStateChangePublisher = Mockito.mock(ControlLoopStateChangePublisher.class);
        var participantRegisterAckPublisher = Mockito.mock(ParticipantRegisterAckPublisher.class);
        var participantDeregisterAckPublisher = Mockito.mock(ParticipantDeregisterAckPublisher.class);
        var participantUpdatePublisher = Mockito.mock(ParticipantUpdatePublisher.class);
        supervisionHandler = new SupervisionHandler(clProvider, participantProvider, monitoringProvider,
                controlLoopUpdatePublisher, controlLoopStateChangePublisher, participantRegisterAckPublisher,
                participantDeregisterAckPublisher, participantUpdatePublisher);
    }

    @AfterAll
    public static void closeDbProvider() throws PfModelException {
        clProvider.close();
        modelsProvider.close();
    }

    @Test
    void testReceiveParticipantRegister() throws Exception {
        final ParticipantRegister participantRegisterMsg = new ParticipantRegister();
        participantRegisterMsg.setParticipantId(getParticipantId());
        participantRegisterMsg.setTimestamp(Instant.now());
        participantRegisterMsg.setParticipantType(getParticipantType());

        synchronized (lockit) {
            ParticipantRegisterListener participantRegisterListener =
                    new ParticipantRegisterListener(supervisionHandler);
            ToscaServiceTemplate serviceTemplate = yamlTranslator.fromYaml(
                    ResourceUtils.getResourceAsString(TOSCA_SERVICE_TEMPLATE_YAML), ToscaServiceTemplate.class);

            commissioningProvider.createControlLoopDefinitions(serviceTemplate);
            participantRegisterListener.onTopicEvent(INFRA, TOPIC, null, participantRegisterMsg);
        }
    }

    @Test
    void testSendParticipantRegisterAck() throws Exception {
        final ParticipantRegisterAck participantRegisterAckMsg = new ParticipantRegisterAck();
        participantRegisterAckMsg.setMessage("ParticipantRegisterAck message");
        participantRegisterAckMsg.setResponseTo(UUID.randomUUID());
        participantRegisterAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantRegisterAckPublisher clRegisterAckPublisher = new ParticipantRegisterAckPublisher();
            clRegisterAckPublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            clRegisterAckPublisher.send(participantRegisterAckMsg);
        }
    }

    @Test
    void testReceiveParticipantDeregister() throws Exception {
        final ParticipantDeregister participantDeregisterMsg = new ParticipantDeregister();
        participantDeregisterMsg.setParticipantId(getParticipantId());
        participantDeregisterMsg.setTimestamp(Instant.now());
        participantDeregisterMsg.setParticipantType(getParticipantType());

        synchronized (lockit) {
            ParticipantDeregisterListener participantDeregisterListener =
                    new ParticipantDeregisterListener(supervisionHandler);
            participantDeregisterListener.onTopicEvent(INFRA, TOPIC, null, participantDeregisterMsg);
        }
    }

    @Test
    void testSendParticipantDeregisterAck() throws Exception {
        final ParticipantDeregisterAck participantDeregisterAckMsg = new ParticipantDeregisterAck();
        participantDeregisterAckMsg.setMessage("ParticipantDeregisterAck message");
        participantDeregisterAckMsg.setResponseTo(UUID.randomUUID());
        participantDeregisterAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantDeregisterAckPublisher clDeregisterAckPublisher = new ParticipantDeregisterAckPublisher();
            clDeregisterAckPublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            clDeregisterAckPublisher.send(participantDeregisterAckMsg);
        }
    }

    @Test
    void testSendParticipantUpdate() throws Exception {
        InstantiationUtils.storeToscaServiceTemplate(TOSCA_TEMPLATE_YAML, commissioningProvider);
        commissioningProvider.getToscaServiceTemplate(null, null);

        final ParticipantUpdate participantUpdateMsg = new ParticipantUpdate();
        participantUpdateMsg.setParticipantId(getParticipantId());
        participantUpdateMsg.setTimestamp(Instant.now());
        participantUpdateMsg.setParticipantType(getParticipantType());
        participantUpdateMsg.setTimestamp(Instant.ofEpochMilli(3000));
        participantUpdateMsg.setMessageId(UUID.randomUUID());

        ToscaServiceTemplate toscaServiceTemplate = commissioningProvider.getToscaServiceTemplate(null, null);
        List<ParticipantDefinition> participantDefinitionUpdates = new ArrayList<>();
        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry :
            toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().entrySet()) {
            if (toscaInputEntry.getValue().getType().contains(CONTROL_LOOP_ELEMENT)) {
                ToscaConceptIdentifier clParticipantId;
                try {
                    clParticipantId = CODER.decode(
                            toscaInputEntry.getValue().getProperties().get("participant_id").toString(),
                            ToscaConceptIdentifier.class);
                } catch (CoderException e) {
                    throw new RuntimeException("cannot get ParticipantId from toscaNodeTemplate", e);
                }
                prepareParticipantDefinitionUpdate(clParticipantId, toscaInputEntry.getKey(),
                    toscaInputEntry.getValue(), participantDefinitionUpdates);
            }
        }

        participantUpdateMsg.setParticipantDefinitionUpdates(participantDefinitionUpdates);
        participantUpdateMsg.setToscaServiceTemplate(toscaServiceTemplate);
        synchronized (lockit) {
            ParticipantUpdatePublisher participantUpdatePublisher =
                new ParticipantUpdatePublisher(commissioningProvider);
            participantUpdatePublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            participantUpdatePublisher.send(participantUpdateMsg);
        }
    }

    private void prepareParticipantDefinitionUpdate(ToscaConceptIdentifier clParticipantId, String entryKey,
        ToscaNodeTemplate entryValue, List<ParticipantDefinition> participantDefinitionUpdates) {

        var clDefinition = new ControlLoopElementDefinition();
        clDefinition.setClElementDefinitionId(new ToscaConceptIdentifier(
            entryKey, entryValue.getVersion()));
        clDefinition.setControlLoopElementToscaNodeTemplate(entryValue);
        List<ControlLoopElementDefinition> controlLoopElementDefinitionList = new ArrayList<>();

        if (participantDefinitionUpdates.isEmpty()) {
            participantDefinitionUpdates.add(getParticipantDefinition(clDefinition, clParticipantId,
                controlLoopElementDefinitionList));
        } else {
            boolean participantExists = false;
            for (ParticipantDefinition participantDefinitionUpdate : participantDefinitionUpdates) {
                if (participantDefinitionUpdate.getParticipantId().equals(clParticipantId)) {
                    participantDefinitionUpdate.getControlLoopElementDefinitionList().add(clDefinition);
                    participantExists = true;
                }
            }
            if (!participantExists) {
                participantDefinitionUpdates.add(getParticipantDefinition(clDefinition, clParticipantId,
                    controlLoopElementDefinitionList));
            }
        }
    }

    private ParticipantDefinition getParticipantDefinition(ControlLoopElementDefinition clDefinition,
        ToscaConceptIdentifier clParticipantId,
        List<ControlLoopElementDefinition> controlLoopElementDefinitionList) {
        ParticipantDefinition participantDefinition = new ParticipantDefinition();
        participantDefinition.setParticipantId(clParticipantId);
        controlLoopElementDefinitionList.add(clDefinition);
        participantDefinition.setControlLoopElementDefinitionList(controlLoopElementDefinitionList);
        return participantDefinition;
    }

    @Test
    void testReceiveParticipantUpdateAckMessage() throws Exception {
        final ParticipantUpdateAck participantUpdateAckMsg = new ParticipantUpdateAck();
        participantUpdateAckMsg.setMessage("ParticipantUpdateAck message");
        participantUpdateAckMsg.setResponseTo(UUID.randomUUID());
        participantUpdateAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantUpdateAckListener participantUpdateAckListener =
                    new ParticipantUpdateAckListener(supervisionHandler);
            participantUpdateAckListener.onTopicEvent(INFRA, TOPIC, null, participantUpdateAckMsg);
        }
    }

    private ToscaConceptIdentifier getParticipantId() {
        return new ToscaConceptIdentifier("org.onap.PM_Policy", "1.0.0");
    }

    private ToscaConceptIdentifier getParticipantType() {
        return new ToscaConceptIdentifier("org.onap.policy.controlloop.PolicyControlLoopParticipant", "2.3.1");
    }
}
