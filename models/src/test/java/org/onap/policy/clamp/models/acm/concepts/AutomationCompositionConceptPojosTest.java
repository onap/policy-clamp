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

package org.onap.policy.clamp.models.acm.concepts;

import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.EqualsAndHashCodeMatchRule;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.NoPublicFieldsExceptStaticFinalRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.test.ToStringTester;

/**
 * Class to perform unit tests of all pojos.
 */
class AutomationCompositionConceptPojosTest {

    @Test
    void testPojos() {
        List<PojoClass> pojoClasses =
                PojoClassFactory.getPojoClasses(AutomationCompositionConceptPojosTest.class.getPackageName());

        // @formatter:off
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
        // @formatter:on
    }
}
