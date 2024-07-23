/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.sim.model;

import lombok.Data;

@Data
public class SimConfig {
    private boolean deploySuccess = true;

    private boolean undeploySuccess = true;

    private boolean lockSuccess = true;

    private boolean unlockSuccess = true;

    private boolean deleteSuccess = true;

    private boolean updateSuccess = true;

    private boolean migrateSuccess = true;

    private boolean migratePrecheck = true;

    private boolean prepare = true;

    private boolean review = true;

    private boolean primeSuccess = true;

    private boolean deprimeSuccess = true;

    private int deployTimerMs = 1000;

    private int undeployTimerMs = 1000;

    private int lockTimerMs = 100;

    private int unlockTimerMs = 100;

    private int updateTimerMs = 100;

    private int migrateTimerMs = 100;

    private int migratePrecheckTimerMs = 100;

    private int prepareTimerMs = 100;

    private int reviewTimerMs = 100;

    private int deleteTimerMs = 100;

    private int primeTimerMs = 100;

    private int deprimeTimerMs = 100;
}
