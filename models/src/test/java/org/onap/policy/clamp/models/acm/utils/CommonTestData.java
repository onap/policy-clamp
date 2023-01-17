/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.utils;

import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to hold/create all parameters for test cases.
 *
 */
public class CommonTestData {

    public static final ToscaConceptIdentifier PARTCICIPANT_ID = new ToscaConceptIdentifier("id", "1.2.3");

    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static ToscaConceptIdentifier getParticipantId() {
        return PARTCICIPANT_ID;
    }

    /**
     * Returns participantId for test Jpa cases.
     *
     * @return participant Id
     */
    public static PfConceptKey getJpaParticipantId() {
        return PARTCICIPANT_ID.asConceptKey();
    }

    /**
     * Returns second participantId for test cases.
     *
     * @return participant Id
     */
    public static ToscaConceptIdentifier getRndParticipantId() {
        return new ToscaConceptIdentifier("idDiff", "1.2.3");
    }
}
