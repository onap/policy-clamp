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

package org.onap.policy.clamp.acm.participant.http.main.models;

import java.util.Map;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

@Data
@AllArgsConstructor
public class RestParams {

    @NotNull
    private ToscaConceptIdentifier restRequestId;

    @NotNull
    private String httpMethod;

    @NotNull
    private String path;

    @Min(100)
    @Max(599)
    private int expectedResponse;

    private Map<String, Object> pathParams;

    private Map<String, String> queryParams;

    private String body;

}
