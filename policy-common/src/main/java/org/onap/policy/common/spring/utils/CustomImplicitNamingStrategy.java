/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.common.spring.utils;

import java.io.Serial;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

public class CustomImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

    @Serial
    private static final long serialVersionUID = 8666774028328486896L;

    @Override
    public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
        String name = source.getReferencedColumnName().getText();
        return toIdentifier(name, source.getBuildingContext());
    }
}
