/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.models.base.validation.annotations;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.BeanValidator;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;

class VerifyKeyValidatorTest {
    private static final String IS_A_NULL_KEY = "is a null key";
    private static final String IS_NULL = "is null";
    private static final String KEY_FIELD = "key";
    private static final String STRING_VALUE = "abc";

    @Test
    void testStandardAnnotation() {
        StdAnnotation data = new StdAnnotation();
        data.strValue = STRING_VALUE;
        assertThat(BeanValidator.validate(data).getResult()).isNull();

        data.strValue = null;
        assertThat(BeanValidator.validate(data).getResult()).contains("strValue", "null");
    }

    @Test
    void testVerifyKey() {
        FullKeyAnnot data = new FullKeyAnnot();

        // null key - Jakarta validation will include all constraint violations
        data.key = new PfConceptKey();
        assertThat(BeanValidator.validate(data).getResult())
            .contains(KEY_FIELD, IS_A_NULL_KEY);

        // invalid version - should invoke cascade validation

        // Create object with invalid version using reflection to bypass setter validation
        data.key = new PfConceptKey("abc", "1.0.0"); // Create with valid version first
        try {
            var versionField = PfConceptKey.class.getDeclaredField("version");
            versionField.setAccessible(true);
            versionField.set(data.key, "xyzzy"); // Set invalid version directly
        } catch (Exception e) {
            throw new RuntimeException("Failed to set invalid version for test", e);
        }
        assertThat(BeanValidator.validate("", data).getResult())
            .contains(KEY_FIELD, "version", "xyzzy", "must match");

        // null name
        data.key = new PfConceptKey(PfKey.NULL_KEY_NAME, "2.3.4");
        assertThat(BeanValidator.validate(data).getResult()).contains(KEY_FIELD, "name", IS_NULL);

        // null version
        data.key = new PfConceptKey(STRING_VALUE, PfKey.NULL_KEY_VERSION);
        assertThat(BeanValidator.validate(data).getResult()).contains(KEY_FIELD, "version", IS_NULL);

        // null name, invalid version - should get two messages
        // Create object with invalid version using reflection to bypass setter validation
        data.key = new PfConceptKey("NULL", "1.0.0"); // Create with valid version first
        try {
            var versionField = PfConceptKey.class.getDeclaredField("version");
            versionField.setAccessible(true);
            versionField.set(data.key, "xyzzy"); // Set invalid version directly
        } catch (Exception e) {
            throw new RuntimeException("Failed to set invalid version for test", e);
        }
        assertThat(BeanValidator.validate("", data).getResult())
            .contains(KEY_FIELD, "name", IS_NULL, "version", "xyzzy", "must match");
    }

    @Test
    void testEmptyKeyAnnotation() {
        EmptyKeyAnnot data = new EmptyKeyAnnot();
        data.key = new PfConceptKey(); // totally invalid key

        // should be ok, since no validations are performed
        assertThat(BeanValidator.validate(data).getResult()).isNull();
    }

    @Test
    void testVerifyKeyOnGetters() {
        GetterKeyAnnot data = new GetterKeyAnnot();

        var result = BeanValidator.validate(data);
        assertThat(result.getResult())
                .contains("nullKey", IS_A_NULL_KEY)
                .contains("nullVersionKey", "version", IS_NULL)
                .doesNotContain("validKey");
    }

    public static class StdAnnotation {
        @Getter
        @NotNull
        private String strValue;
    }

    public static class FullKeyAnnot {
        @Getter
        @Valid
        @VerifyKey(keyNotNull = true, nameNotNull = true, versionNotNull = true)
        private PfKey key;
    }

    public static class EmptyKeyAnnot {
        @Getter
        @Valid
        @VerifyKey(keyNotNull = false, nameNotNull = false, versionNotNull = false)
        private PfKey key;
    }

    public static class GetterKeyAnnot {
        @Valid
        @VerifyKey(versionNotNull = true)
        public PfConceptKey getValidKey() {
            return new PfConceptKey("validName", "1.0.0");
        }

        @Valid
        @VerifyKey(versionNotNull = true)
        public PfConceptKey getNullKey() {
            return new PfConceptKey(PfKey.NULL_KEY_NAME, PfKey.NULL_KEY_VERSION);
        }

        @Valid
        @VerifyKey(versionNotNull = true)
        public PfConceptKey getNullVersionKey() {
            return new PfConceptKey("validName", PfKey.NULL_KEY_VERSION);
        }
    }
}
