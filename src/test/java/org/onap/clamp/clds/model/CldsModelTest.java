/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.model;

import org.onap.clamp.clds.model.CldsModel;
import org.jboss.resteasy.spi.BadRequestException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test org.onap.clamp.ClampDesigner.model.Model
 */
public class CldsModelTest {

    @Test
    public void testCreateUsingControlName() {
        utilCreateUsingControlName("abc-", "7c42aceb-2350-11e6-8131-fa163ea8d2da");
        utilCreateUsingControlName("", "7c42aceb-2350-11e6-8131-fa163ea8d2da");
    }

    @Test(expected = BadRequestException.class)
    public void testExceptionCreateUsingControlName() {
        utilCreateUsingControlName("", "c42aceb-2350-11e6-8131-fa163ea8d2da");
    }

    public void utilCreateUsingControlName(String controlNamePrefix, String controlNameUuid) {
        CldsModel model = CldsModel.createUsingControlName(controlNamePrefix + controlNameUuid);
        assertEquals(controlNamePrefix, model.getControlNamePrefix());
        assertEquals(controlNameUuid, model.getControlNameUuid());
    }
}
