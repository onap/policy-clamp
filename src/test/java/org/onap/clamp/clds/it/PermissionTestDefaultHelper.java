/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Samsung. All rights reserved.
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

package org.onap.clamp.clds.it;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class PermissionTestDefaultHelper extends PermissionTestHelper {

    private static final String[] ALL_ACTION = new String[] { "*" };
    private static final String[] READ_UPDATE_ACTION = new String[] { "read", "update" };

    private static final String DEV_INSTANCE = "dev";
    private static final String TEST_INSTANCE = "test";

    private static final Map<String, Map<?, ?>> defaultPermission = ImmutableMap.of("permission-type-cl",
            ImmutableMap.of(DEV_INSTANCE, ALL_ACTION), "permission-type-cl-event",
            ImmutableMap.of(DEV_INSTANCE, ALL_ACTION, TEST_INSTANCE, READ_UPDATE_ACTION), "permission-type-cl-manage",
            ImmutableMap.of(DEV_INSTANCE, ALL_ACTION, TEST_INSTANCE, READ_UPDATE_ACTION), "permission-type-filter-vf",
            ImmutableMap.of(DEV_INSTANCE, ALL_ACTION, TEST_INSTANCE, READ_UPDATE_ACTION), "permission-type-template",
            ImmutableMap.of(DEV_INSTANCE, ALL_ACTION, TEST_INSTANCE, READ_UPDATE_ACTION));

    /**
     * Permission test default helper constructor. This class setup the default
     * permission in the parent PermissionTestHelper class.
     */
    public PermissionTestDefaultHelper() {
        super(defaultPermission);
    }
}
