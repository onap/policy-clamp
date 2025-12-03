/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation
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

package org.onap.policy.common.gson.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LifterTest {

    private static Gson gson = new Gson();

    @Test
    void testLifter_testShouldLift() throws Exception {
        Set<String> set = new HashSet<>(Arrays.asList("abc", "def"));
        Lifter lifter = new Lifter(gson, set, LifterTest.class.getDeclaredMethod("getValue"), String.class);

        // should not lift these
        assertFalse(lifter.shouldLift("abc"));
        assertFalse(lifter.shouldLift("def"));

        // should lift anything else
        assertTrue(lifter.shouldLift("hello"));
        assertTrue(lifter.shouldLift("world"));
    }

    public String getValue() {
        return "";
    }

}
