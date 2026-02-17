/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.common.utils.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openpojo.reflection.impl.PojoClassFactory;
import lombok.ToString;
import org.junit.jupiter.api.Test;

class ToStringTesterTest {

    @Test
    void testGoodToString() {
        final ToStringTester tester = new ToStringTester();
        final var pojoClass = PojoClassFactory.getPojoClass(GoodToStringClass.class);
        assertDoesNotThrow(() -> tester.run(pojoClass));
    }

    @Test
    void testDefaultToString() {
        final ToStringTester tester = new ToStringTester();
        final var pojoClass = PojoClassFactory.getPojoClass(DefaultToStringClass.class);
        assertThrows(AssertionError.class, () -> tester.run(pojoClass));
    }

    @ToString
    static class GoodToStringClass {
        String myField;
    }

    static class DefaultToStringClass {
    }
}
