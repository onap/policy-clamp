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
    /** The control loop is not initialized on participants. */
    UNINITIALIZED,
    /** The control loop is instantiated but is passive. */
    PASSIVE,
    /** The control loop is running and is ready to execute control loops. */
    READY,
    /** The control loop is running and is executing control loops. */
    EXECUTING,
    /** The control loop has been ordered to go passive. */
    PASSIVATING;
}
