/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.common.utils.test;

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
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for testing POJO classes using OpenPojo validation framework.
 * Validates getter/setter methods, equals/hashCode contracts, and other POJO conventions.
 */
@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class PojoTester {

    /**
     * Tests all POJOs in the specified package using default exclusion pattern.
     *
     * @param packageName the package to scan for POJO classes
     */
    public static void testPojos(String packageName) {
        testPojos(packageName, ".*(Test|Utils|Converter|Comparator)$");
    }

    /**
     * Tests all POJOs in the specified package, excluding classes matching the pattern.
     *
     * @param packageName the package to scan for POJO classes
     * @param excludePattern regex pattern for class names to exclude
     */
    public static void testPojos(String packageName, String excludePattern) {
        var pojoClasses = PojoClassFactory.getPojoClassesRecursively(packageName, new FilterNonConcrete());
        pojoClasses.removeIf(clazz -> clazz.getName().matches(excludePattern));
        if (pojoClasses.isEmpty()) {
            throw new IllegalArgumentException("No POJO classes found in package: " + packageName);
        }
        pojoClasses.forEach(clazz -> log.info("Testing class: {}", clazz.getName()));

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
