/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.policy.concepts;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Batch modification of a deployment group, which groups multiple DeploymentSubGroup
 * entities together for a particular domain.
 */
@Data
@NoArgsConstructor
public class DeploymentGroup {
    private static final String SUBGROUP_FIELD = "deploymentSubgroups";

    private String name;
    private List<DeploymentSubGroup> deploymentSubgroups;
}
