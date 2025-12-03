/*-
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

package org.onap.policy.common.utils.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeatureApiUtilsTest {
    private static final String HANDLED = "handled";

    private MyPred pred;
    private List<String> tried;
    private List<String> errors;

    /**
     * Initializes fields.
     */
    @BeforeEach
    public void setUp() {
        tried = new LinkedList<>();
        errors = new LinkedList<>();
        pred = new MyPred();
    }

    @Test
    void testApplyFeatureTrue() {
        assertTrue(FeatureApiUtils.apply(Arrays.asList("exceptT0", "falseT1", HANDLED, "falseT2", HANDLED), pred,
            (str, ex) -> errors.add(str)));

        assertEquals("[exceptT0, falseT1, handled]", tried.toString());
        assertEquals("[exceptT0]", errors.toString());
    }

    @Test
    void testApplyFeatureFalse() {
        List<String> lst = Arrays.asList("falseF1", "exceptF2", "falseF3");

        assertFalse(FeatureApiUtils.apply(lst, pred, (str, ex) -> errors.add(str)));
        assertEquals(lst.toString(), tried.toString());
        assertEquals("[exceptF2]", errors.toString());
    }

    private class MyPred implements Predicate<String> {

        @Override
        public boolean test(String data) {
            tried.add(data);

            if (data.startsWith("except")) {
                throw new IllegalArgumentException("expected exception");
            }

            return data.equals(HANDLED);
        }
    }
}
