/*
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.controlloop.common;

/**
 * Names of various items contained in the Registry.
 */
public class ControlLoopConstants {

    // Registry keys
    public static final String REG_CLRUNTIME_ACTIVATOR = "object:activator/clruntime";

    public static final String REG_CLRUNTIME_DAO_FACTORY = "object:clruntime/dao/factory";
    public static final String REG_STATISTICS_MANAGER = "object:manager/statistics";
    public static final String REG_PARTICIPANT_MODIFY_LOCK = "lock:participant";
    public static final String REG_PARTICIPANT_MODIFY_MAP = "object:participant/modify/map";
    public static final String REG_PARTICIPANT_TRACKER = "object:participant/tracker";
    public static final String REG_PARTICIPANT_NOTIFIER = "object:participant/notifier";

    // Topic names
    public static final String TOPIC_POLICY_CLRUNTIME_PARTICIPANT = "POLICY-CLRUNTIME-PARTICIPANT";
    public static final String TOPIC_POLICY_NOTIFICATION = "POLICY-NOTIFICATION";

    private ControlLoopConstants() {
        super();
    }
}
