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
 * Filter class for searches for {@link ControlLoop} instances. If any fields are null, they are ignored.
 */
@Builder
@Data
public class ControlLoopFilter implements PfObjectFilter<ControlLoop> {
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
    public List<ControlLoop> filter(@NonNull final List<ControlLoop> originalList) {

        // @formatter:off
        List<ControlLoop> returnList = originalList.stream()
                .filter(filterStringPred(name, ControlLoop::getName))
                .filter(filterStringPred((LATEST_VERSION.equals(version) ? null : version), ControlLoop::getVersion))
                .filter(filterPrefixPred(versionPrefix, ControlLoop::getVersion))
                .filter(filterStringPred(definitionName, ControlLoop::getDefinitionName))
                .filter(filterStringPred(
                    (LATEST_VERSION.equals(definitionVersionPrefix) ? null :
                        definitionVersionPrefix), ControlLoop::getDefinitionVersion))
                .filter(filterPrefixPred(definitionVersionPrefix, ControlLoop::getDefinitionVersion))
                .collect(Collectors.toList());
        // @formatter:off

        if (LATEST_VERSION.equals(version)) {
            return this.latestVersionFilter(returnList, new ControlLoopComparator());
        } else  {
            return returnList;
        }
    }
}
