/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.models.tosca.authorative.concepts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ToscaRequirementTest {

    @Test
    void testCopyConstructor() {
        // Create original object and populate fields
        ToscaRequirement original = new ToscaRequirement();
        original.setCapability("capabilityA");
        original.setNode("nodeA");
        original.setRelationship("relationshipA");
        original.setOccurrences(List.of("occurrence1", "occurrence2"));

        // Use copy constructor
        ToscaRequirement copy = new ToscaRequirement(original);

        // Validate copied values
        assertEquals(original.getCapability(), copy.getCapability());
        assertEquals(original.getNode(), copy.getNode());
        assertEquals(original.getRelationship(), copy.getRelationship());
        assertEquals(original.getOccurrences(), copy.getOccurrences());
    }

}
