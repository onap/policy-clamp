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

package org.onap.policy.clamp.controlloop.models.controlloop.concepts;

import java.util.Map;
import javax.ws.rs.core.Response;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public final class ParticipantUtils {

    private static final Coder CODER = new StandardCoder();

    private ParticipantUtils() {
        // Utility class has no instances
    }

    /**
     * Finds participantType from a map of properties.
     *
     * @param properties Map of properties
     * @return participantType
     */
    public static ToscaConceptIdentifier findParticipantType(Map<String, Object> properties) {
        var objParticipantType = properties.get("participantType");
        if (objParticipantType != null) {
            try {
                return CODER.decode(objParticipantType.toString(), ToscaConceptIdentifier.class);
            } catch (CoderException e) {
                throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, e.getMessage(), e);
            }
        }
        return null;
    }
}
