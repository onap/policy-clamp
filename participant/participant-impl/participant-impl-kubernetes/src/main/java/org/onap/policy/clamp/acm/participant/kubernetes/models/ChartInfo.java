/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2021 Nordix Foundation. All rights reserved.
 * ======================================================================
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
 * ========================LICENSE_END===================================
 */

package org.onap.policy.clamp.acm.participant.kubernetes.models;

import java.util.Map;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

@Data
@RequiredArgsConstructor
public class ChartInfo {

    @NonNull
    private String releaseName;

    @NonNull
    private ToscaConceptIdentifier chartId;

    @NonNull
    private String namespace;

    private HelmRepository repository;

    private Map<String, String> overrideParams;

}
