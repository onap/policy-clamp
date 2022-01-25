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

package org.onap.policy.clamp.models.acm.messages.dmaap.participant;

import static org.junit.Assert.assertEquals;

import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * Utility class for tests of ParticipantMessage subclasses.
 */
public class ParticipantMessageUtils {

    private ParticipantMessageUtils() {

    }

    public static String removeVariableFields(String text) {
        return text.replaceAll("messageId=[^,]*", "messageId=xxx").replaceAll("timestamp=[^,]*", "timestamp=nnn");
    }

    /**
     * Check if object is Serializable.
     *
     * @param object the Object
     * @param clazz the class of the Object
     * @throws CoderException if object is not Serializable
     */
    public static <T> void assertSerializable(Object object, Class<T> clazz) throws CoderException {
        var standardCoder = new StandardCoder();
        var json = standardCoder.encode(object);
        var other = standardCoder.decode(json, clazz);

        assertEquals(removeVariableFields(object.toString()), removeVariableFields(other.toString()));
    }
}
