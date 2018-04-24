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
 * 
 */

package org.onap.clamp.clds.client.req;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.onap.clamp.clds.client.req.policy.OperationalPolicyReq;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.sdc.Resource;
import org.onap.policy.sdc.ResourceType;

public class OperationalPolicyReqTest {

    @Test
    public void convertToResourceTest() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Method method = OperationalPolicyReq.class.getDeclaredMethod("convertToResource", List.class,
                ResourceType.class);
        method.setAccessible(true);
        // return method.invoke(targetObject, argObjects);
        List<String> stringList = new ArrayList<>();
        stringList.add("test1");
        stringList.add("test2");
        stringList.add("test3");
        stringList.add("test4");
        Resource[] resources = (Resource[]) method.invoke(null, stringList, ResourceType.VF);

        assertTrue(resources.length == 4);
        assertTrue("test1".equals(resources[0].getResourceName()));
        assertTrue("test2".equals(resources[1].getResourceName()));
        assertTrue("test3".equals(resources[2].getResourceName()));
        assertTrue("test4".equals(resources[3].getResourceName()));
    }

    @Test
    public void convertToPolicyResultTest() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Method method = OperationalPolicyReq.class.getDeclaredMethod("convertToPolicyResult", List.class);
        method.setAccessible(true);
        // return method.invoke(targetObject, argObjects);
        List<String> stringList = new ArrayList<>();
        stringList.add("FAILURE");
        stringList.add("SUCCESS");
        stringList.add("FAILURE_GUARD");
        stringList.add("FAILURE_TIMEOUT");
        PolicyResult[] policyResult = (PolicyResult[]) method.invoke(null, stringList);

        assertTrue(policyResult.length == 4);
        assertTrue(policyResult[0].equals(PolicyResult.FAILURE));
        assertTrue(policyResult[1].equals(PolicyResult.SUCCESS));
        assertTrue(policyResult[2].equals(PolicyResult.FAILURE_GUARD));
        assertTrue(policyResult[3].equals(PolicyResult.FAILURE_TIMEOUT));
    }
}
