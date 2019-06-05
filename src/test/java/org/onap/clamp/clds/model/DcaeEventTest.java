/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Samsung. All rights reserved.
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
 *
 */

package org.onap.clamp.clds.model;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import javax.ws.rs.BadRequestException;
import java.util.Arrays;

public class DcaeEventTest {

    @Test
    public void testGetCldsActionId() {
        //given
        DcaeEvent dcaeEvent = new DcaeEvent();
        dcaeEvent.setEvent(DcaeEvent.EVENT_CREATED);
        dcaeEvent.setResourceUUID("1");
        dcaeEvent.setServiceUUID("2");

        //when
        String cldsAction = dcaeEvent.getCldsActionCd();
        dcaeEvent.setInstances(Arrays.asList(new CldsModelInstance()));
        //then
        assertEquals(CldsEvent.ACTION_CREATE, cldsAction);

        //when
        dcaeEvent.setEvent(DcaeEvent.EVENT_DEPLOYMENT);
        //then
        assertEquals(CldsEvent.ACTION_DEPLOY, dcaeEvent.getCldsActionCd());

        //when
        dcaeEvent.setInstances(null);
        //then
        assertEquals(CldsEvent.ACTION_DEPLOY, dcaeEvent.getCldsActionCd());

        //when
        dcaeEvent.setEvent(DcaeEvent.EVENT_UNDEPLOYMENT);
        //then
        assertEquals(CldsEvent.ACTION_UNDEPLOY, dcaeEvent.getCldsActionCd());

    }

    @Test(expected = BadRequestException.class)
    public void shouldReturnBadRequestException() {
        //given
        DcaeEvent dcaeEvent = new DcaeEvent();
        dcaeEvent.setResourceUUID("1");
        dcaeEvent.setServiceUUID("2");
        //when
        dcaeEvent.setEvent("BadEvent");
        //then
        dcaeEvent.getCldsActionCd();
    }
}
