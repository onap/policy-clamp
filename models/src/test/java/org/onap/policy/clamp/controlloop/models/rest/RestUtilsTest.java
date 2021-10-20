/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.rest;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.errors.concepts.ErrorResponse;
import org.onap.policy.models.errors.concepts.ErrorResponseInfo;

class RestUtilsTest {

    private static final String MESSAGE_ERROR = "Erorr";
    private static final Status STATUS_ERROR = Status.BAD_REQUEST;

    @Test
    void testToSimpleResponse() {
        var ex = new ErrorResponseInfo() {

            @Override
            public ErrorResponse getErrorResponse() {
                var er = new ErrorResponse();
                er.setErrorMessage(MESSAGE_ERROR);
                er.setResponseCode(STATUS_ERROR);
                return er;
            }
        };

        var response = RestUtils.toSimpleResponse(ex);

        assertThat(response.getStatusCodeValue()).isEqualTo(STATUS_ERROR.getStatusCode());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorDetails()).isEqualTo(MESSAGE_ERROR);
    }
}
