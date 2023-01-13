/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.persistence.concepts;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;

/**
 * Test the {@link JpaParticipantSupportedElementType} class.
 */
public class JpaParticipantSupportedElementTypeTest {

    private static final String NULL_PARTICIPANT_ID_ERROR = "participantId is marked .*ull but is null";
    private static final String NULL_ID_ERROR = "id is marked .*ull but is null";
    private static final String NULL_ERROR = " is marked .*ull but is null";
    private static final String ID = "a95757ba-b34a-4049-a2a8-46773abcbe5e";
    private static final String PARTICIPANT_ID = "a78757co-b34a-8949-a2a8-46773abcbe2a";

    @Test
    void testJpaAutomationCompositionElementConstructor() {
        assertThatThrownBy(() -> {
            new JpaParticipantSupportedElementType((JpaParticipantSupportedElementType) null);
        }).hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaParticipantSupportedElementType("key", null);
        }).hasMessageMatching(NULL_PARTICIPANT_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaParticipantSupportedElementType(null, "key");
        }).hasMessageMatching(NULL_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaParticipantSupportedElementType(null, null);
        }).hasMessageMatching(NULL_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaParticipantSupportedElementType(null, null, null, null);
        }).hasMessageMatching(NULL_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaParticipantSupportedElementType("key", null, null, null);
        }).hasMessageMatching(NULL_PARTICIPANT_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaParticipantSupportedElementType("key", "key", null, "1.0.0");
        }).hasMessageMatching("typeName" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaParticipantSupportedElementType("key", "key", "name", null);
        }).hasMessageMatching("typeVersion" + NULL_ERROR);

        assertNotNull(new JpaParticipantSupportedElementType());
        assertNotNull(new JpaParticipantSupportedElementType("key", "key"));
        assertNotNull(new JpaParticipantSupportedElementType("key", "key", "name",
            "1.0.0"));
    }

    @Test
    void testJpaParticipantSupportedElementType() {
        var testJpaSupportElement = createJpaParticipantSupportedElementType();

        var testSupportedElement = createParticipantSupportedElementType();
        assertEquals(testSupportedElement.getId(), testJpaSupportElement.toAuthorative().getId());
        assertEquals(testSupportedElement.getTypeName(), testJpaSupportElement.toAuthorative().getTypeName());
        assertEquals(testSupportedElement.getTypeVersion(), testJpaSupportElement.toAuthorative().getTypeVersion());

        assertThatThrownBy(() -> {
            testJpaSupportElement.fromAuthorative(null);
        }).hasMessageMatching("participantSupportedElementType is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipantSupportedElementType((JpaParticipantSupportedElementType) null))
            .isInstanceOf(NullPointerException.class);

        var testJpaSupportElementFa =
            new JpaParticipantSupportedElementType(testSupportedElement.getId().toString(),
                testJpaSupportElement.getParticipantId());
        testJpaSupportElementFa.fromAuthorative(testSupportedElement);
        assertEquals(testJpaSupportElement, testJpaSupportElementFa);

        assertEquals(ID, testJpaSupportElement.getId());

        var testJpaSupportElement2 = new JpaParticipantSupportedElementType(testJpaSupportElement);
        assertEquals(testJpaSupportElement, testJpaSupportElement2);
    }

    @Test
    void testJpaAutomationCompositionElementCompareTo() {
        var testJpaSupportElement = createJpaParticipantSupportedElementType();

        var otherJpaSupportElement =
            new JpaParticipantSupportedElementType(testJpaSupportElement);
        assertEquals(0, testJpaSupportElement.compareTo(otherJpaSupportElement));
        assertEquals(-1, testJpaSupportElement.compareTo(null));
        assertEquals(0, testJpaSupportElement.compareTo(testJpaSupportElement));

        testJpaSupportElement.setId("BadValue");
        assertNotEquals(0, testJpaSupportElement.compareTo(otherJpaSupportElement));
        testJpaSupportElement.setId(ID);
        assertEquals(0, testJpaSupportElement.compareTo(otherJpaSupportElement));

        testJpaSupportElement.setParticipantId("BadValue");
        assertNotEquals(0, testJpaSupportElement.compareTo(otherJpaSupportElement));
        testJpaSupportElement.setParticipantId(PARTICIPANT_ID);
        assertEquals(0, testJpaSupportElement.compareTo(otherJpaSupportElement));

        testJpaSupportElement.setTypeName("BadName");
        assertNotEquals(0, testJpaSupportElement.compareTo(otherJpaSupportElement));
        testJpaSupportElement.setTypeName("type");
        assertEquals(0, testJpaSupportElement.compareTo(otherJpaSupportElement));

        testJpaSupportElement.setTypeVersion("BadVersion");
        assertNotEquals(0, testJpaSupportElement.compareTo(otherJpaSupportElement));
        testJpaSupportElement.setTypeVersion("1.0.0");
        assertEquals(0, testJpaSupportElement.compareTo(otherJpaSupportElement));

        assertEquals(testJpaSupportElement,
            new JpaParticipantSupportedElementType(otherJpaSupportElement));
    }

    private JpaParticipantSupportedElementType createJpaParticipantSupportedElementType() {
        var testSupportedElement = createParticipantSupportedElementType();
        var testJpaSupportElement = new JpaParticipantSupportedElementType(testSupportedElement.getId().toString(),
            PARTICIPANT_ID);

        testJpaSupportElement.fromAuthorative(testSupportedElement);

        return testJpaSupportElement;
    }

    private ParticipantSupportedElementType createParticipantSupportedElementType() {
        var testSupportedElement = new ParticipantSupportedElementType();
        testSupportedElement.setId(UUID.fromString(ID));
        testSupportedElement.setTypeName("type");
        testSupportedElement.setTypeVersion("1.0.0");

        return testSupportedElement;
    }
}
