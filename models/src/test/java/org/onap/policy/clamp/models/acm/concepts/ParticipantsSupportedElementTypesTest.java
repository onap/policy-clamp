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

package org.onap.policy.clamp.models.acm.concepts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class ParticipantsSupportedElementTypesTest {

    private static final String ID = "a95757ba-b34a-4049-a2a8-46773abcbe5e";
    private static final String PARTICIPANT_ID = "a78757co-b34a-8949-a2a8-46773abcbe2a";

    @Test
    void testParticipant() {

        ParticipantSupportedElementType p0 = new ParticipantSupportedElementType();
        p0.setId(UUID.fromString(ID));
        assertEquals(ID, p0.getId().toString());

        ParticipantSupportedElementType p1 = new ParticipantSupportedElementType(p0);

        assertThat(p0).usingRecursiveComparison().isEqualTo(p1);
    }

    @Test
    void testParticipantLombok() {
        assertNotNull(new ParticipantSupportedElementType());
        ParticipantSupportedElementType p0 = new ParticipantSupportedElementType();

        assertThat(p0.toString()).contains("org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType");
        assertThat(p0.hashCode()).isNotZero();
        assertThat(p0).usingRecursiveComparison().isEqualTo(new ParticipantSupportedElementType(p0));
        assertNotEquals(null, p0);


        ParticipantSupportedElementType p1 = new ParticipantSupportedElementType();

        p1.setId(UUID.fromString(ID));
        p1.setTypeName("name");
        p1.setTypeVersion("1.0.0");

        assertThat(p1.toString()).contains("org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType");
        assertNotEquals(0, p1.hashCode());
        assertNotEquals(p1, p0);
        assertNotEquals(null, p1);


        ParticipantSupportedElementType p2 = new ParticipantSupportedElementType();
        p2.setId(p0.getId());

        assertThat(p0).usingRecursiveComparison().isEqualTo(p2);
    }
}
