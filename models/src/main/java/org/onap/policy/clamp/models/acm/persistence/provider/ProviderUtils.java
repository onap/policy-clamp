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

package org.onap.policy.clamp.models.acm.persistence.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConcept;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaEntity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProviderUtils {

    /**
     * Convert a list of concepts to a list of Jpa objects.
     *
     * @param authorativeConceptList the list of concepts
     * @param jpaSupplier the Jpa Supplier
     * @param conceptDescription the description used for validation result
     * @return the list of Jpa objects
     */
    public static <A, J extends PfConcept & PfAuthorative<A>> List<J> getJpaAndValidateList(
            List<A> authorativeConceptList, Supplier<J> jpaSupplier, String conceptDescription) {
        var validationResult = new BeanValidationResult(conceptDescription + " List", authorativeConceptList);

        List<J> jpaConceptList = new ArrayList<>(authorativeConceptList.size());

        for (A authorativeConcept : authorativeConceptList) {
            var jpaConcept = jpaSupplier.get();
            jpaConcept.fromAuthorative(authorativeConcept);
            jpaConceptList.add(jpaConcept);

            validationResult.addResult(jpaConcept.validate(conceptDescription));
        }

        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }
        return jpaConceptList;
    }

    /**
     * Convert a concept to a Jpa object.
     *
     * @param authorativeConcept the concept
     * @param jpaSupplier  the Jpa Supplier
     * @param conceptDescription the description used for validation result
     * @return the Jpa object
     */
    public static <A, J extends PfConcept & PfAuthorative<A>> J getJpaAndValidate(A authorativeConcept,
            Supplier<J> jpaSupplier, String conceptDescription) {
        var validationResult = new BeanValidationResult(conceptDescription, authorativeConcept);

        var jpaConcept = jpaSupplier.get();
        jpaConcept.fromAuthorative(authorativeConcept);

        validationResult.addResult(jpaConcept.validate(conceptDescription));

        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }
        return jpaConcept;
    }

    /**
     * Convert JPA automation composition list to an authorative automation composition list.
     *
     * @param <T> the type of TOSCA entity
     * @param <J> the type of JPA TOSCA entity
     * @param jpaEntityList the list to convert
     * @return the authorative list
     */
    public static <T extends ToscaEntity, J extends PfAuthorative<T>> List<T> asEntityList(List<J> jpaEntityList) {
        return jpaEntityList.stream().map(J::toAuthorative).collect(Collectors.toList());
    }
}
