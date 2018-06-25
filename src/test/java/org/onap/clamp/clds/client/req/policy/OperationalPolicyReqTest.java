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

package org.onap.clamp.clds.client.req.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.sdc.Resource;
import org.onap.policy.sdc.ResourceType;

public class OperationalPolicyReqTest {

    @Test
    public void shouldConvertGivenStringsToResourceObjects()
        throws NoSuchMethodException, SecurityException, IllegalAccessException,
        IllegalArgumentException, InvocationTargetException {

        //given
        List<String> stringList = Arrays.asList("test1", "test2", "test3", "test4");

        //when
        Resource[] resources = OperationalPolicyReq.convertToResource(stringList, ResourceType.VF);

        //then
        assertThat(resources).extracting(Resource::getResourceName)
            .containsExactly("test1", "test2", "test3", "test4");
    }

    @Test
    public void shouldConvertGivenStringsToPolicyResults()
        throws NoSuchMethodException, SecurityException, IllegalAccessException,
        IllegalArgumentException, InvocationTargetException {
        //given
        List<String> stringList = Arrays.asList("FAILURE", "SUCCESS", "FAILURE_GUARD", "FAILURE_TIMEOUT");

        //when
        PolicyResult[] policyResults = OperationalPolicyReq.convertToPolicyResult(stringList);

        //then
        assertThat(policyResults)
            .containsExactly(PolicyResult.FAILURE, PolicyResult.SUCCESS,
                PolicyResult.FAILURE_GUARD, PolicyResult.FAILURE_TIMEOUT);
    }
}
