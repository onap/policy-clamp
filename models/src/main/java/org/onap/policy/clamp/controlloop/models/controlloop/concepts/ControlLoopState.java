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

package org.onap.policy.clamp.controlloop.models.controlloop.concepts;

public enum ControlLoopState {
    /**
     * The control loop or control loop element is not initialised on participants, it does not exist on participants.
     */
    UNINITIALISED,
    /**
     * The control loop or control loop element is changing from unitialised to passive, it is being initialised onto
     * participants.
     */
    UNINITIALISED2PASSIVE,
    /**
     * The control loop or control loop element is initialised on the participants but is passive, that is, it is not
     * handling control loop messages yet.
     */
    PASSIVE,
    /**
     * The control loop or control loop element is changing from passive to running, the participants are preparing to
     * execute control loops.
     */
    PASSIVE2RUNNING,
    /** The control loop or control loop element is running and is executing control loops. */
    RUNNING,
    /**
     * The control loop or control loop element is completing execution of current control loops but will not start
     * running any more control loops and will become passive.
     */
    RUNNING2PASSIVE,
    /**
     * The control loop or control loop element is changing from passive to unitialised, the control loop is being
     * removed from participants.
     */
    PASSIVE2UNINITIALISED;

    public boolean equalsControlLoopOrderedState(final ControlLoopOrderedState controlLoopOrderedState)  {
        return this.name().equals(controlLoopOrderedState.name());
    }

    public ControlLoopOrderedState asOrderedState() {
        return ControlLoopOrderedState.valueOf(this.name());
    }
}
