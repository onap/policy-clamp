/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.runtime.main.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcmParameters;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

class EncryptionUtilsTest {

    private EncryptionUtils encryptionUtilsEnabled;
    private EncryptionUtils encryptionUtilsDisabled;

    @BeforeEach
    void setup() {
        AcRuntimeParameterGroup acRuntimeParameterGroup = mock(AcRuntimeParameterGroup.class);
        AcmParameters acmParameters = mock(AcmParameters.class);
        when(acRuntimeParameterGroup.getAcmParameters()).thenReturn(acmParameters);

        when(acmParameters.isEnableEncryption()).thenReturn(true);
        encryptionUtilsEnabled = new EncryptionUtils(acRuntimeParameterGroup);

        when(acmParameters.isEnableEncryption()).thenReturn(false);
        encryptionUtilsDisabled = new EncryptionUtils(acRuntimeParameterGroup);
    }

    @Test
    void testEncryptionEnabledFlag() {
        assertThat(encryptionUtilsEnabled.encryptionEnabled()).isTrue();
        assertThat(encryptionUtilsDisabled.encryptionEnabled()).isFalse();
    }

    @Test
    void testEncryptDecrypt_topLevelProperty() {
        // Build a definition with a nodeTemplate and a nodeType whose property is marked sensitive
        ToscaProperty sensitiveProp = new ToscaProperty();
        sensitiveProp.setName("password");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("sensitive", "true");
        sensitiveProp.setMetadata(metadata);
        Map<String, ToscaProperty> props = new HashMap<>();
        props.put("password", sensitiveProp);

        ToscaNodeType nodeType = new ToscaNodeType();
        nodeType.setName("MyType");
        nodeType.setProperties(props);

        ToscaNodeTemplate nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setName("MyNode");
        nodeTemplate.setType("MyType");

        // build service template containers
        Map<String, ToscaNodeType> nodeTypes = new HashMap<>();
        nodeTypes.put("MyType", nodeType);
        Map<String, ToscaNodeTemplate> nodeTemplates = new HashMap<>();
        nodeTemplates.put("MyNode", nodeTemplate);

        // service template wrapper classes used by your implementation
        ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();
        serviceTemplate.setNodeTypes(nodeTypes);
        serviceTemplate.setToscaTopologyTemplate(new ToscaTopologyTemplate());
        serviceTemplate.getToscaTopologyTemplate().setNodeTemplates(nodeTemplates);

        AutomationCompositionDefinition acDefinition = new AutomationCompositionDefinition();
        acDefinition.setServiceTemplate(serviceTemplate);

        // automation composition instance with one element that refers to the node template name MyNode
        AutomationCompositionElementDefinition def = new AutomationCompositionElementDefinition();
        var toscaDefinition = new ToscaConceptIdentifier("MyNode", "1.0.0");
        def.setAcElementDefinitionId(toscaDefinition);

        AutomationCompositionElement element = new AutomationCompositionElement();
        var id = UUID.randomUUID();
        element.setId(id);
        element.setDefinition(toscaDefinition);
        Map<String, Object> elementProps = new HashMap<>();
        elementProps.put("password", "topSecret");
        element.setProperties(elementProps);

        def.setAutomationCompositionElementToscaNodeTemplate(nodeTemplate);

        AutomationComposition ac = new AutomationComposition();
        Map<UUID, AutomationCompositionElement> elements = new HashMap<>();
        elements.put(element.getId(), element);
        ac.setElements(elements);

        // run encryption
        encryptionUtilsEnabled.findAndEncryptSensitiveData(acDefinition, ac);

        // property should be encrypted
        var after = (String) ac.getElements().get(id).getProperties().get("password");
        assertThat(after).startsWith("ENCRYPTED:");

        // now decrypt via public API
        encryptionUtilsEnabled.decryptInstanceProperties(ac);
        var decrypted = (String) ac.getElements().get(id).getProperties().get("password");
        assertThat(decrypted).isEqualTo("topSecret");
    }

    @Test
    void testEncryptDecrypt_nestedMapList() {
        // Prepare similar types but place the sensitive property nested in a list of maps
        ToscaProperty sensitiveProp = new ToscaProperty();
        sensitiveProp.setName("token");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("sensitive", "true");
        sensitiveProp.setMetadata(metadata);
        Map<String, ToscaProperty> props = new HashMap<>();
        props.put("token", sensitiveProp);

        ToscaNodeType nodeType = new ToscaNodeType();
        nodeType.setName("Type2");
        nodeType.setProperties(props);

        ToscaNodeTemplate nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setName("Node2");
        nodeTemplate.setType(nodeType.getName());

        Map<String, ToscaNodeType> nodeTypes = new HashMap<>();
        nodeTypes.put(nodeType.getName(), nodeType);
        Map<String, ToscaNodeTemplate> nodeTemplates = new HashMap<>();
        nodeTemplates.put(nodeTemplate.getName(), nodeTemplate);

        ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();
        serviceTemplate.setName("ServiceTemplate2");
        serviceTemplate.setNodeTypes(nodeTypes);
        serviceTemplate.setToscaTopologyTemplate(new ToscaTopologyTemplate());
        serviceTemplate.getToscaTopologyTemplate().setNodeTemplates(nodeTemplates);

        AutomationComposition ac = new AutomationComposition();
        ac.setCompositionId(UUID.randomUUID());

        AutomationCompositionDefinition acDefinition = new AutomationCompositionDefinition();
        acDefinition.setServiceTemplate(serviceTemplate);
        acDefinition.setCompositionId(ac.getCompositionId());

        AutomationCompositionElement element = new AutomationCompositionElement();
        element.setId(UUID.randomUUID());
        element.setDefinition(new ToscaConceptIdentifier("Node2", "1.0.0"));

        // nested structure: properties -> list -> map -> token
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("token", "listSecret");
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(innerMap);
        Map<String, Object> propsInstance = new HashMap<>();
        propsInstance.put("someList", list);
        Map<UUID, AutomationCompositionElement> elements = new HashMap<>();
        element.setProperties(propsInstance);
        elements.put(element.getId(), element);
        ac.setElements(elements);

        encryptionUtilsEnabled.findAndEncryptSensitiveData(acDefinition, ac);

        // confirm if the nested value was encrypted
        var storedList = (List<?>) ac.getElements().get(element.getId()).getProperties().get("someList");
        var storedMap = (Map<?, ?>) storedList.get(0);
        var storedVal = (String) storedMap.get("token");
        assertThat(storedVal).startsWith("ENCRYPTED:");

        // decrypt
        encryptionUtilsEnabled.decryptInstanceProperties(ac);
        var after = (Map<?, ?>) ((List<?>) ac.getElements().get(element.getId())
            .getProperties().get("someList")).get(0);
        assertEquals("listSecret", after.get("token"));
    }

    @Test
    void testDecrypt_invalidCipher_throwsRuntimeException() {
        // create a composition with a property that has a malformed ENCRYPTED: value
        var elementId = UUID.randomUUID();
        AutomationCompositionElement element = new AutomationCompositionElement();
        element.setId(elementId);
        Map<String, Object> props = new HashMap<>();
        props.put("bad", "ENCRYPTED:invalidbase64!!");
        element.setProperties(props);

        AutomationComposition ac = new AutomationComposition();
        Map<UUID, AutomationCompositionElement> elements = new HashMap<>();
        elements.put(element.getId(), element);
        ac.setElements(elements);

        // expect exception when decrypt called (Base64 decode -> fail -> AutomationCompositionRuntimeException)
        assertThatThrownBy(() -> encryptionUtilsEnabled.decryptInstanceProperties(ac))
            .isInstanceOf(AutomationCompositionRuntimeException.class)
            .hasMessageContaining("Failed to decrypt instance field");
    }

    @Test
    void testDecryptInstanceProperties_listVersion_and_disabled() {
        // Disabled should not attempt to decrypt (no exception thrown)
        var elementId = UUID.randomUUID();
        AutomationCompositionElement element = new AutomationCompositionElement();
        element.setId(elementId);
        Map<String, Object> props = new HashMap<>();
        props.put("field", "ENCRYPTED:whatever");
        element.setProperties(props);

        AutomationComposition ac = new AutomationComposition();
        Map<UUID, AutomationCompositionElement> elements = new HashMap<>();
        elements.put(element.getId(), element);
        ac.setElements(elements);

        List<AutomationComposition> list = new ArrayList<>();
        list.add(ac);

        // should not throw when encryption disabled
        assertDoesNotThrow(() -> encryptionUtilsDisabled.decryptInstanceProperties(list));
    }

    @Test
    void testFilterSensitiveProperties_datatypeRef() {
        // create a property that references a data type by name and data type contains sensitive property
        ToscaProperty propRef = new ToscaProperty();
        propRef.setName("p1");
        propRef.setType("MyDataTypeName");

        Map<String, ToscaProperty> nodeProps = new HashMap<>();
        nodeProps.put("p1", propRef);

        ToscaNodeType nodeType = new ToscaNodeType();
        nodeType.setName("NodeTypeA");
        nodeType.setProperties(nodeProps);

        ToscaDataType dataType = new ToscaDataType();
        dataType.setName("MyDataTypeName");
        ToscaProperty nested = new ToscaProperty();
        nested.setName("secretNested");
        Map<String, String> meta = new HashMap<>();
        meta.put("sensitive", "true");
        nested.setMetadata(meta);
        Map<String, ToscaProperty> dtProps = new HashMap<>();
        dtProps.put("secretNested", nested);
        dataType.setProperties(dtProps);

        ToscaNodeTemplate nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setName("NodeInstance");
        nodeTemplate.setType("NodeTypeA");

        ToscaServiceTemplate ser = new ToscaServiceTemplate();
        ser.setDataTypes(Collections.singletonMap(dataType.getName(), dataType));
        ser.setNodeTypes(Collections.singletonMap(nodeType.getName(), nodeType));
        ser.setToscaTopologyTemplate(new ToscaTopologyTemplate());
        ser.getToscaTopologyTemplate().setNodeTemplates(Collections.singletonMap(nodeTemplate.getName(), nodeTemplate));

        AutomationComposition composition = new AutomationComposition();
        composition.setCompositionId(UUID.randomUUID());
        AutomationCompositionDefinition acDefinition = new AutomationCompositionDefinition();
        acDefinition.setServiceTemplate(ser);
        acDefinition.setCompositionId(composition.getCompositionId());
        AutomationCompositionElement acElement = new AutomationCompositionElement();
        acElement.setId(UUID.randomUUID());
        acElement.setDefinition(new ToscaConceptIdentifier("NodeInstance", "1.0.0"));
        composition.setElements(Collections.singletonMap(acElement.getId(), acElement));

        List<ToscaNodeType> nodeTypes = Collections.singletonList(nodeType);
        List<ToscaDataType> dataTypes = Collections.singletonList(dataType);
        List<ToscaNodeTemplate> nodeTemplates = Collections.singletonList(nodeTemplate);

        var found = encryptionUtilsEnabled.filterSensitiveProperties(acElement, nodeTypes, dataTypes, nodeTemplates);
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getName()).isEqualTo("secretNested");
    }

    @Test
    void testGetCipher_Roundtrip() throws Exception {
        // ensure getCipher can be used to encrypt/decrypt bytes with same iv and key
        byte[] iv = new byte[12];
        // create encrypt cipher
        Cipher encryptCipher = encryptionUtilsEnabled.getCipher(iv, Cipher.ENCRYPT_MODE);
        byte[] plain = "plainText".getBytes();
        byte[] cipherBytes = encryptCipher.doFinal(plain);

        // combine iv + cipherBytes as the class does
        ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherBytes.length);
        buf.put(iv);
        buf.put(cipherBytes);
        byte[] combined = buf.array();
        var encoded = "ENCRYPTED:" + Base64.getEncoder().encodeToString(combined);

        // decrypt using the class' decrypt method by invoking decryptInstanceProperties flow:
        AutomationCompositionElement element = new AutomationCompositionElement();
        var elementId = UUID.randomUUID();
        element.setId(elementId);
        Map<String, Object> props = new HashMap<>();
        props.put("x", encoded);
        element.setProperties(props);

        AutomationComposition ac = new AutomationComposition();
        Map<UUID, AutomationCompositionElement> elements = new HashMap<>();
        elements.put(element.getId(), element);
        ac.setElements(elements);

        // Should decrypt successfully to original plaintext
        encryptionUtilsEnabled.decryptInstanceProperties(ac);
        assertEquals("plainText", ac.getElements().get(elementId).getProperties().get("x"));
    }
}

