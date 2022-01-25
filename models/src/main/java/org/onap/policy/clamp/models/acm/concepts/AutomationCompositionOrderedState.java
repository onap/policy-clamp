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

public enum AutomationCompositionOrderedState {
    /**
     * The automation composition or automation composition element should become uninitialised on participants, it
     * should not exist on participants.
     */
    UNINITIALISED,
    /**
     * The automation composition or automation composition element should initialised on the participants and be
     * passive, that is, it is not handling automation composition messages yet.
     */
    PASSIVE,
    /**
     * The automation composition or automation composition element should running and is executing automation
     * compositions.
     */
    RUNNING;

    public boolean equalsAutomationCompositionState(final AutomationCompositionState automationCompositionState) {
        return this.name().equals(automationCompositionState.name());
    }

    public AutomationCompositionState asState() {
        return AutomationCompositionState.valueOf(this.name());
    }
}
