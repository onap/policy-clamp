/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.common.parameters.topic;

import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Class to hold topic details such as name, server and topicCommInfrastructure.
 *
 * @author Ajith Sreekumar (ajith.sreekumar@est.tech)
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TopicParameters extends BusTopicParams {
    @NotBlank
    private String topicCommInfrastructure;

    public TopicParameters() {
        // this defaults to true
        setManaged(true);
    }
}
