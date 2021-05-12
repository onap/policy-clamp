/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.exception.sdc.controller;

/**
 * Exception during Csar operations.
 */
public class CsarHandlerException extends Exception {

    /**
     * The serial version ID.
     */
    private static final long serialVersionUID = -7628640776124409155L;

    /**
     * This constructor can be used to create a new CsarHandlerException.
     *
     * @param message The message to dump
     */
    public CsarHandlerException(final String message) {
        super(message);
    }

    /**
     * This constructor can be used to create a new CsarHandlerException.
     *
     * @param message The message to dump
     * @param cause The Throwable cause object
     */
    public CsarHandlerException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
