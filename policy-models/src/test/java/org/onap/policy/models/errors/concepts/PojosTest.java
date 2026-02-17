/*-
 * ============LICENSE_START=======================================================
 * ONAP Policy Decision Models
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.models.errors.concepts;

import com.openpojo.reflection.filters.FilterNonConcrete;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.EqualsAndHashCodeMatchRule;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.NoPublicFieldsExceptStaticFinalRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.test.ToStringTester;

class PojosTest {

    @Test
    void testPojos() {
        var pojoClasses = PojoClassFactory.getPojoClassesRecursively(getClass().getPackageName(),
                new FilterNonConcrete());
        pojoClasses.removeIf(clazz -> clazz.getName().matches(".*(Test|Utils|Converter|Comparator)$"));
        pojoClasses.forEach(clazz -> System.out.println("Testing class: " + clazz.getName()));

        final Validator validator = ValidatorBuilder
                .create()
                .with(new SetterMustExistRule())
                .with(new GetterMustExistRule())
                .with(new EqualsAndHashCodeMatchRule())
                .with(new NoPublicFieldsExceptStaticFinalRule())
                .with(new SetterTester())
                .with(new GetterTester())
                .with(new ToStringTester())
                .build();

        validator.validate(pojoClasses);
    }

}