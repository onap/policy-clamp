/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2016-2018 Ericsson. All rights reserved.
 *  Modifications Copyright (C) 2019-2024 Nordix Foundation.
 *  Modifications Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.utils.validation;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * The Class ResourceUtilsTest.
 *
 * @author Liam Fallon (liam.fallon@ericsson.com)
 */
class AssertionsTest {
    private static final String HELLO = "Hello";
    private static final String IT_IS_OK = "it is OK";
    private static final String IT_IS_NULL = "it is null";

    @Test
    void testAssertions() {
        Assertions.argumentNotNull(HELLO, IT_IS_OK);

        assertThatIllegalArgumentException().isThrownBy(() -> Assertions.argumentNotNull(null, IT_IS_NULL))
                        .withMessage(IT_IS_NULL);


        Assertions.instanceOf(HELLO, String.class);

        assertThatIllegalArgumentException().isThrownBy(() -> Assertions.instanceOf(100, String.class))
                        .withMessage("java.lang.Integer is not an instance of java.lang.String");


        Assertions.validateStringParameter("name", "MyName", "^M.*e$");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> Assertions.validateStringParameter("name", "MyName", "^M.*f$"))
            .withMessage("parameter \"name\": value \"MyName\", does not match regular expression \"^M.*f$\"");
    }
}
