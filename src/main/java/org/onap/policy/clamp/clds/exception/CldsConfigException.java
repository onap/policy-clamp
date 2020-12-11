/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.exception;

/**
 * New exception to CldsUser errors.
 *
 */
public class CldsConfigException extends RuntimeException {

    /**
     * The serial version ID.
     */
    private static final long serialVersionUID = 5958873136187918505L;

    /**
     * This constructor can be used to create a new CldsConfigException.
     * 
     * @param message
     *            A string message detailing the problem
     * @param ex
     *            The exception sent by the code
     */
    public CldsConfigException(String message, Throwable ex) {
        super(message, ex);
    }

    /**
     * This constructor can be used to create a new CldsConfigException. Use
     * this constructor only if you are creating a new exception stack, not if
     * an exception was already raised by another code.
     *
     * @param message
     *            A string message detailing the problem
     */
    public CldsConfigException(String message) {
        super(message);
    }

}
