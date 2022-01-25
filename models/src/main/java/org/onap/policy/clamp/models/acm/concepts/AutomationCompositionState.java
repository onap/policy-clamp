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

package org.onap.policy.clamp.models.acm.concepts;

public enum AutomationCompositionState {
    /**
     * The automation composition or automation composition element is not initialised on participants, it does not
     * exist on participants.
     */
    UNINITIALISED,
    /**
     * The automation composition or automation composition element is changing from unitialised to passive, it is being
     * initialised onto participants.
     */
    UNINITIALISED2PASSIVE,
    /**
     * The automation composition or automation composition element is initialised on the participants but is passive,
     * that is, it is not handling automation composition messages yet.
     */
    PASSIVE,
    /**
     * The automation composition or automation composition element is changing from passive to running, the
     * participants are preparing to execute automation compositions.
     */
    PASSIVE2RUNNING,
    /**
     * The automation composition or automation composition element is running and is executing automation compositions.
     */
    RUNNING,
    /**
     * The automation composition or automation composition element is completing execution of current automation
     * compositions but will not start running any more automation compositions and will become passive.
     */
    RUNNING2PASSIVE,
    /**
     * The automation composition or automation composition element is changing from passive to unitialised, the
     * automation composition is being removed from participants.
     */
    PASSIVE2UNINITIALISED;

    public boolean equalsAutomationCompositionOrderedState(
        final AutomationCompositionOrderedState automationCompositionOrderedState) {
        return this.name().equals(automationCompositionOrderedState.name());
    }

    public AutomationCompositionOrderedState asOrderedState() {
        return AutomationCompositionOrderedState.valueOf(this.name());
    }
}
