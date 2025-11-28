/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 *  Modifications Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * A deployment (i.e., set of policies) for all PDPs of the same pdp type running within a
 * particular domain.
 */
@Data
@NoArgsConstructor
public class DeploymentSubGroup {

    public enum Action {
        POST,       // all listed policies are to be added
        DELETE,     // all listed policies are to be deleted
        PATCH       // update the deployment so that the policies match exactly
    }

    private String pdpType;
    private Action action;
    private List<ToscaConceptIdentifier> policies;
}
