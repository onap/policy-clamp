/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts.JpaControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.repository.ControlLoopRepository;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.repository.ToscaNodeTemplateRepository;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.repository.ToscaNodeTemplatesRepository;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.YamlJsonTranslator;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaNodeTemplate;

class ControlLoopProviderTest {

    private static final String LIST_IS_NULL = "controlLoops is marked .*ull but is null";
    private static final String OBJECT_IS_NULL = "controlLoop is marked non-null but is null";

    private static final String ID_NAME = "PMSHInstance1";
    private static final String ID_VERSION = "1.0.1";
    private static final String ID_NAME_NOT_EXTST = "not_exist";
    private static final String ID_NAME_NOT_VALID = "not_valid";

    private static final Coder CODER = new StandardCoder();
    private static final String CONTROL_LOOP_JSON = "src/test/resources/providers/TestControlLoops.json";
    private static final String TOSCA_TEMPLATE_YAML = "examples/controlloop/PMSubscriptionHandling.yaml";

    private static final YamlJsonTranslator yamlTranslator = new YamlJsonTranslator();

    private ControlLoops inputControlLoops;
    private List<JpaControlLoop> inputControlLoopsJpa;
    private String originalJson = ResourceUtils.getResourceAsString(CONTROL_LOOP_JSON);

    @BeforeEach
    void beforeSetupDao() throws Exception {
        inputControlLoops = CODER.decode(originalJson, ControlLoops.class);
        inputControlLoopsJpa = ProviderUtils.getJpaAndValidateList(inputControlLoops.getControlLoopList(),
                JpaControlLoop::new, "control loops");
    }

    @Test
    void testControlLoopsSave() throws Exception {
        var controlLoopRepository = mock(ControlLoopRepository.class);
        var controlLoopProvider = new ControlLoopProvider(controlLoopRepository,
                mock(ToscaNodeTemplateRepository.class), mock(ToscaNodeTemplatesRepository.class));

        assertThatThrownBy(() -> {
            controlLoopProvider.saveControlLoops(null);
        }).hasMessageMatching(LIST_IS_NULL);

        when(controlLoopRepository.saveAll(inputControlLoopsJpa)).thenReturn(inputControlLoopsJpa);

        var createdControlLoops = new ControlLoops();
        createdControlLoops
                .setControlLoopList(controlLoopProvider.saveControlLoops(inputControlLoops.getControlLoopList()));

        assertEquals(inputControlLoops, createdControlLoops);

        when(controlLoopRepository.saveAll(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> {
            controlLoopProvider.saveControlLoops(inputControlLoops.getControlLoopList());
        }).hasMessageMatching("Error in save ControlLoops");
    }

    @Test
    void testControlLoopSave() throws Exception {
        var controlLoopRepository = mock(ControlLoopRepository.class);
        var controlLoopProvider = new ControlLoopProvider(controlLoopRepository,
                mock(ToscaNodeTemplateRepository.class), mock(ToscaNodeTemplatesRepository.class));

        assertThatThrownBy(() -> {
            controlLoopProvider.saveControlLoop(null);
        }).hasMessageMatching(OBJECT_IS_NULL);

        when(controlLoopRepository.save(inputControlLoopsJpa.get(0))).thenReturn(inputControlLoopsJpa.get(0));

        var createdControlLoop = controlLoopProvider.saveControlLoop(inputControlLoops.getControlLoopList().get(0));

        assertEquals(inputControlLoops.getControlLoopList().get(0), createdControlLoop);

        when(controlLoopRepository.save(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> {
            controlLoopProvider.saveControlLoop(inputControlLoops.getControlLoopList().get(0));
        }).hasMessageMatching("Error in save controlLoop");
    }

    @Test
    void testGetControlLoops() throws Exception {
        var controlLoopRepository = mock(ControlLoopRepository.class);
        var controlLoopProvider = new ControlLoopProvider(controlLoopRepository,
                mock(ToscaNodeTemplateRepository.class), mock(ToscaNodeTemplatesRepository.class));

        // Return empty list when no data present in db
        List<ControlLoop> getResponse = controlLoopProvider.getControlLoops();
        assertThat(getResponse).isEmpty();

        controlLoopProvider.saveControlLoops(inputControlLoops.getControlLoopList());

        var controlLoop0 = inputControlLoops.getControlLoopList().get(1);
        var name = controlLoop0.getName();
        var version = controlLoop0.getVersion();
        var controlLoop1 = inputControlLoops.getControlLoopList().get(1);

        when(controlLoopRepository.getFiltered(eq(JpaControlLoop.class), any(), any()))
                .thenReturn(List.of(new JpaControlLoop(controlLoop0), new JpaControlLoop(controlLoop1)));
        when(controlLoopRepository.findById(controlLoop0.getKey().asIdentifier().asConceptKey()))
                .thenReturn(Optional.of(new JpaControlLoop(controlLoop0)));
        when(controlLoopRepository.getById(controlLoop0.getKey().asIdentifier().asConceptKey()))
                .thenReturn(new JpaControlLoop(controlLoop0));
        when(controlLoopRepository.getFiltered(JpaControlLoop.class, name, version))
                .thenReturn(List.of(new JpaControlLoop(controlLoop0)));
        when(controlLoopRepository.findById(controlLoop1.getKey().asIdentifier().asConceptKey()))
                .thenReturn(Optional.of(new JpaControlLoop(controlLoop1)));

        assertEquals(1, controlLoopProvider.getControlLoops(name, version).size());

        var cl = controlLoopProvider.findControlLoop(new ToscaConceptIdentifier(ID_NAME, ID_VERSION)).get();
        assertEquals(inputControlLoops.getControlLoopList().get(1), cl);

        cl = controlLoopProvider.getControlLoop(new ToscaConceptIdentifier(ID_NAME, ID_VERSION));
        assertEquals(inputControlLoops.getControlLoopList().get(1), cl);

        when(controlLoopRepository.getById(any())).thenThrow(EntityNotFoundException.class);

        assertThatThrownBy(() -> {
            controlLoopProvider.getControlLoop(new ToscaConceptIdentifier(ID_NAME_NOT_EXTST, ID_VERSION));
        }).hasMessageMatching("ControlLoop not found");

        cl = controlLoopProvider.findControlLoop(ID_NAME, ID_VERSION).get();
        assertEquals(inputControlLoops.getControlLoopList().get(1), cl);

        assertThat(controlLoopProvider.findControlLoop(new ToscaConceptIdentifier(ID_NAME_NOT_EXTST, ID_VERSION)))
                .isEmpty();

        when(controlLoopRepository.findById(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> {
            controlLoopProvider.findControlLoop(ID_NAME_NOT_VALID, ID_VERSION);
        }).hasMessageMatching("Not valid parameter");
    }

    @Test
    void testDeleteControlLoop() throws Exception {
        var controlLoopRepository = mock(ControlLoopRepository.class);
        var controlLoopProvider = new ControlLoopProvider(controlLoopRepository,
                mock(ToscaNodeTemplateRepository.class), mock(ToscaNodeTemplatesRepository.class));

        assertThatThrownBy(() -> {
            controlLoopProvider.deleteControlLoop(ID_NAME_NOT_EXTST, ID_VERSION);
        }).hasMessageMatching(".*.failed, control loop does not exist");

        var controlLoop = inputControlLoops.getControlLoopList().get(0);
        var name = controlLoop.getName();
        var version = controlLoop.getVersion();

        when(controlLoopRepository.findById(new PfConceptKey(name, version)))
                .thenReturn(Optional.of(inputControlLoopsJpa.get(0)));

        ControlLoop deletedCl = controlLoopProvider.deleteControlLoop(name, version);
        assertEquals(controlLoop, deletedCl);
    }

    @Test
    void testDeleteAllInstanceProperties() throws Exception {
        var controlLoopProvider = new ControlLoopProvider(mock(ControlLoopRepository.class),
                mock(ToscaNodeTemplateRepository.class), mock(ToscaNodeTemplatesRepository.class));
        var toscaServiceTemplate = testControlLoopRead();
        controlLoopProvider.deleteInstanceProperties(controlLoopProvider.saveInstanceProperties(toscaServiceTemplate),
                controlLoopProvider.getNodeTemplates(null, null));
        assertThat(controlLoopProvider.getControlLoops()).isEmpty();
    }

    @Test
    void testSaveAndDeleteInstanceProperties() throws Exception {
        var toscaNodeTemplatesRepository = mock(ToscaNodeTemplatesRepository.class);
        var toscaNodeTemplateRepository = mock(ToscaNodeTemplateRepository.class);
        var controlLoopProvider = new ControlLoopProvider(mock(ControlLoopRepository.class),
                toscaNodeTemplateRepository, toscaNodeTemplatesRepository);
        var toscaServiceTest = testControlLoopRead();

        controlLoopProvider.saveInstanceProperties(toscaServiceTest);
        verify(toscaNodeTemplatesRepository).save(any());

        var name = "org.onap.policy.controlloop.PolicyControlLoopParticipant";
        var version = "2.3.1";
        var elem = toscaServiceTest.getToscaTopologyTemplate().getNodeTemplates().get(name);
        when(toscaNodeTemplateRepository.getFiltered(JpaToscaNodeTemplate.class, name, version))
                .thenReturn(List.of(new JpaToscaNodeTemplate(elem)));

        var filtered = controlLoopProvider.getNodeTemplates(name, version);
        verify(toscaNodeTemplateRepository).getFiltered(JpaToscaNodeTemplate.class, name, version);

        controlLoopProvider.deleteInstanceProperties(controlLoopProvider.saveInstanceProperties(toscaServiceTest),
                filtered);

        verify(toscaNodeTemplateRepository).delete(any());
    }

    @Test
    void testGetNodeTemplates() throws Exception {
        var toscaNodeTemplateRepository = mock(ToscaNodeTemplateRepository.class);
        var controlLoopProvider = new ControlLoopProvider(mock(ControlLoopRepository.class),
                toscaNodeTemplateRepository, mock(ToscaNodeTemplatesRepository.class));

        var toscaNodeTemplate0 = new JpaToscaNodeTemplate(new PfConceptKey(ID_NAME, ID_VERSION));
        var toscaNodeTemplate1 = new JpaToscaNodeTemplate(new PfConceptKey("PMSHInstance2", ID_VERSION));

        when(toscaNodeTemplateRepository.getFiltered(JpaToscaNodeTemplate.class, null, null))
                .thenReturn(List.of(toscaNodeTemplate0, toscaNodeTemplate1));
        when(toscaNodeTemplateRepository.findAll()).thenReturn(List.of(toscaNodeTemplate0, toscaNodeTemplate1));
        when(toscaNodeTemplateRepository.getFiltered(JpaToscaNodeTemplate.class, ID_NAME, ID_VERSION))
                .thenReturn(List.of(toscaNodeTemplate0));

        // Getting all nodes
        var listNodes = controlLoopProvider.getNodeTemplates(null, null);
        assertNotNull(listNodes);
        assertThat(listNodes).hasSize(2);

        listNodes = controlLoopProvider.getNodeTemplates(ID_NAME, ID_VERSION);
        assertNotNull(listNodes);
        assertThat(listNodes).hasSize(1);

        listNodes = controlLoopProvider.getAllNodeTemplates();
        assertNotNull(listNodes);
        assertThat(listNodes).hasSize(2);

        var nodeTemplateFilter =
                ToscaTypedEntityFilter.<ToscaNodeTemplate>builder().name(ID_NAME).version(ID_VERSION).build();

        listNodes = controlLoopProvider.getFilteredNodeTemplates(nodeTemplateFilter);
        assertNotNull(listNodes);
        assertThat(listNodes).hasSize(1);

        assertThatThrownBy(() -> {
            controlLoopProvider.getFilteredNodeTemplates(null);
        }).hasMessageMatching("filter is marked non-null but is null");
    }

    private static ToscaServiceTemplate testControlLoopRead() {
        return testControlLoopYamlSerialization(TOSCA_TEMPLATE_YAML);
    }

    private static ToscaServiceTemplate testControlLoopYamlSerialization(String controlLoopFilePath) {
        var controlLoopString = ResourceUtils.getResourceAsString(controlLoopFilePath);
        return yamlTranslator.fromYaml(controlLoopString, ToscaServiceTemplate.class);
    }
}
