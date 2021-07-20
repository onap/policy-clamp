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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
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
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.clamp.controlloop.runtime.util.rest.CommonRestController;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
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
    private static final long interval = 1000;
    private static SupervisionHandler supervisionHandler;
    private static CommissioningProvider commissioningProvider;
    private static ControlLoopProvider clProvider;
    private static PolicyModelsProvider modelsProvider;
    private static final YamlJsonTranslator yamlTranslator = new YamlJsonTranslator();

    /**
     * setup Db Provider Parameters.
     *
     * @throws PfModelException if an error occurs
     */
    @BeforeAll
    public static void setupDbProviderParameters() throws PfModelException {
        ClRuntimeParameterGroup controlLoopParameters = CommonTestData.geParameterGroup("instantproviderdb");

        modelsProvider =
                CommonTestData.getPolicyModelsProvider(controlLoopParameters.getDatabaseProviderParameters());
        clProvider = new ControlLoopProvider(controlLoopParameters.getDatabaseProviderParameters());
        var participantStatisticsProvider =
                new ParticipantStatisticsProvider(controlLoopParameters.getDatabaseProviderParameters());
        var clElementStatisticsProvider =
                new ClElementStatisticsProvider(controlLoopParameters.getDatabaseProviderParameters());
        commissioningProvider = new CommissioningProvider(modelsProvider, clProvider);
        var monitoringProvider =
                new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);
        var participantProvider = new ParticipantProvider(controlLoopParameters.getDatabaseProviderParameters());
        var controlLoopUpdatePublisher = Mockito.mock(ParticipantControlLoopUpdatePublisher.class);
        var controlLoopStateChangePublisher = Mockito.mock(ParticipantControlLoopStateChangePublisher.class);
        var participantRegisterAckPublisher = Mockito.mock(ParticipantRegisterAckPublisher.class);
        var participantDeregisterAckPublisher = Mockito.mock(ParticipantDeregisterAckPublisher.class);
        var participantUpdatePublisher = Mockito.mock(ParticipantUpdatePublisher.class);
        supervisionHandler = new SupervisionHandler(clProvider, participantProvider, monitoringProvider,
                        commissioningProvider, controlLoopUpdatePublisher, controlLoopStateChangePublisher,
                        participantRegisterAckPublisher, participantDeregisterAckPublisher, participantUpdatePublisher);
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
            ToscaServiceTemplate serviceTemplate = yamlTranslator
                .fromYaml(ResourceUtils.getResourceAsString(TOSCA_SERVICE_TEMPLATE_YAML), ToscaServiceTemplate.class);

            List<ToscaNodeTemplate> listOfTemplates = commissioningProvider.getControlLoopDefinitions(null, null);
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
        final ParticipantUpdate participantUpdateMsg = new ParticipantUpdate();
        participantUpdateMsg.setParticipantId(getParticipantId());
        participantUpdateMsg.setTimestamp(Instant.now());
        participantUpdateMsg.setParticipantType(getParticipantType());
        participantUpdateMsg.setTimestamp(Instant.ofEpochMilli(3000));
        participantUpdateMsg.setMessageId(UUID.randomUUID());

        ToscaServiceTemplate toscaServiceTemplate = new ToscaServiceTemplate();
        toscaServiceTemplate.setName("serviceTemplate");
        toscaServiceTemplate.setDerivedFrom("parentServiceTemplate");
        toscaServiceTemplate.setDescription("Description of serviceTemplate");
        toscaServiceTemplate.setVersion("1.2.3");

        ControlLoopElementDefinition clDefinition = new ControlLoopElementDefinition();
        clDefinition.setId(UUID.randomUUID());
        clDefinition.setControlLoopElementToscaServiceTemplate(toscaServiceTemplate);
        Map<String, String> commonPropertiesMap = Map.of("Prop1", "PropValue");
        clDefinition.setCommonPropertiesMap(commonPropertiesMap);

        Map<UUID, ControlLoopElementDefinition> controlLoopElementDefinitionMap =
            Map.of(UUID.randomUUID(), clDefinition);

        Map<ToscaConceptIdentifier, Map<UUID, ControlLoopElementDefinition>>
            participantDefinitionUpdateMap = Map.of(getParticipantId(), controlLoopElementDefinitionMap);
        participantUpdateMsg.setParticipantDefinitionUpdateMap(participantDefinitionUpdateMap);

        synchronized (lockit) {
            ParticipantUpdatePublisher clUpdatePublisher = new ParticipantUpdatePublisher();
            clUpdatePublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            clUpdatePublisher.send(participantUpdateMsg);
        }
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
