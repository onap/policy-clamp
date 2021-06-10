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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConcept;
import org.onap.policy.models.base.PfModelRuntimeException;

public final class ProviderUtils {
    private ProviderUtils() {
        // Utility class has no instances
    }

    protected static <A, J extends PfConcept & PfAuthorative<A>> List<J> getJpaAndValidate(
            List<A> authorativeConceptList, Class<J> jpaClass, String conceptDescription) {
        var validationResult = new BeanValidationResult(conceptDescription + " List", authorativeConceptList);

        List<J> jpaConceptList = new ArrayList<>(authorativeConceptList.size());

        for (A authorativeConcept : authorativeConceptList) {
            var jpaConcept = intantiateJpaClass(jpaClass, conceptDescription);
            jpaConcept.fromAuthorative(authorativeConcept);
            jpaConceptList.add(jpaConcept);

            validationResult.addResult(jpaConcept.validate(conceptDescription));
        }

        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }
        return jpaConceptList;
    }

    private static <A, J extends PfConcept & PfAuthorative<A>> J intantiateJpaClass(Class<J> jpaClass,
            String conceptDescription) throws PfModelRuntimeException {

        try {
            return jpaClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new PfModelRuntimeException(Response.Status.INTERNAL_SERVER_ERROR,
                    "could not instantiate JPA class for " + conceptDescription);
        }
    }
}
