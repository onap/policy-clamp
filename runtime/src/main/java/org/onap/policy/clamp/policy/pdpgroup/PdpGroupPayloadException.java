/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 *
 */

package org.onap.policy.clamp.policy.pdpgroup;

/**
 * Exception during Pdp Group payload construction.
 */
public class PdpGroupPayloadException extends Exception {

    /**
     * serialization id.
     */
    private static final long serialVersionUID = -5676848693241134101L;

    /**
     * This constructor can be used to create a new PdpGroupPayloadException.
     *
     * @param message The message to dump
     */
    public PdpGroupPayloadException(final String message) {
        super(message);
    }

    /**
     * This constructor can be used to create a new PdpGroupPayloadException.
     *
     * @param message The message to dump
     * @param cause The Throwable cause object
     */
    public PdpGroupPayloadException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
