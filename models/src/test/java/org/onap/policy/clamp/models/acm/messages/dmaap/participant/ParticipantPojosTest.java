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

package org.onap.policy.clamp.models.acm.messages.dmaap.participant;

import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.test.ToStringTester;

/**
 * Class to perform unit tests of all pojos.
 */
class ParticipantPojosTest {

    @Test
    void testPojos() {
        List<PojoClass> pojoClasses =
                PojoClassFactory.getPojoClasses(ParticipantPojosTest.class.getPackageName());

        pojoClasses.remove(PojoClassFactory.getPojoClass(ParticipantMessage.class));
        pojoClasses.remove(PojoClassFactory.getPojoClass(ParticipantMessageTest.class));
        pojoClasses.remove(PojoClassFactory.getPojoClass(ParticipantAckMessage.class));
        pojoClasses.remove(PojoClassFactory.getPojoClass(ParticipantAckMessageTest.class));
        pojoClasses.remove(PojoClassFactory.getPojoClass(AutomationCompositionAck.class));
        pojoClasses.remove(PojoClassFactory.getPojoClass(AutomationCompositionAckTest.class));

        // @formatter:off
        final Validator validator = ValidatorBuilder
                .create()
                .with(new ToStringTester())
                .with(new SetterMustExistRule())
                .with(new GetterMustExistRule())
                .with(new SetterTester())
                .with(new GetterTester())
                .build();

        validator.validate(pojoClasses);
        // @formatter:on
    }
}
