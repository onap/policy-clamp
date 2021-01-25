/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.concepts;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.onap.policy.models.base.PfObjectFilter;

/**
 * Filter class for searches for {@link Participant} instances. If any fields are null, they are ignored.
 */
@Builder
@Data
public class ParticipantFilter implements PfObjectFilter<Participant> {
    public static final String LATEST_VERSION = "LATEST";

    // Exact expression
    private String name;

    // Exact match, set to LATEST_VERSION to get the latest version
    private String version;

    // version prefix
    private String versionPrefix;

    // Exact expression
    private String definitionName;

    // Exact Expression, set to LATEST_VERSION to get the latest version
    private String definitionVersionPrefix;

    @Override
    public List<Participant> filter(@NonNull final List<Participant> originalList) {

        // @formatter:off
        List<Participant> returnList = originalList.stream()
                .filter(filterStringPred(name, Participant::getName))
                .filter(filterStringPred((LATEST_VERSION.equals(version) ? null : version), Participant::getVersion))
                .filter(filterPrefixPred(versionPrefix, Participant::getVersion))
                .filter(filterStringPred(definitionName, Participant::getDefinitionName))
                .filter(filterStringPred(
                    (LATEST_VERSION.equals(definitionVersionPrefix) ? null :
                        definitionVersionPrefix), Participant::getDefinitionVersion))
                .filter(filterPrefixPred(definitionVersionPrefix, Participant::getDefinitionVersion))
                .collect(Collectors.toList());
        // @formatter:off

        if (LATEST_VERSION.equals(version)) {
            return this.latestVersionFilter(returnList, new ParticipantComparator());
        } else  {
            return returnList;
        }
    }
}
