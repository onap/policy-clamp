/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
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
 * Modifications copyright (c) 2019 AT&T
 * ===================================================================
 *
 */

package org.onap.clamp.clds.util.drawing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.gson.JsonObject;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.clamp.loop.Loop;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;

@RunWith(MockitoJUnitRunner.class)
public class ClampGraphBuilderTest {
    @Mock
    private Painter mockPainter;

    @Captor
    private ArgumentCaptor<String> collectorCaptor;

    @Captor
    private ArgumentCaptor<Set<MicroServicePolicy>> microServicesCaptor;

    @Captor
    private ArgumentCaptor<Set<OperationalPolicy>> policyCaptor;

    /**
     * Do a quick test of the graphBuilder chain.
     */
    @Test
    public void clampGraphBuilderCompleteChainTest() {
        String collector = "VES";
        MicroServicePolicy ms1 = new MicroServicePolicy("ms1", new PolicyModel("org.onap.ms1", "", "1.0.0"), false,
                null);
        MicroServicePolicy ms2 = new MicroServicePolicy("ms2", new PolicyModel("org.onap.ms2", "", "1.0.0"), false,
                null);

        OperationalPolicy opPolicy = new OperationalPolicy("OperationalPolicy", new Loop(), new JsonObject(),
                new PolicyModel("org.onap.opolicy", null, "1.0.0", "opolicy1"));
        final Set<OperationalPolicy> opPolicies = Set.of(opPolicy);
        final Set<MicroServicePolicy> microServices = Set.of(ms1, ms2);

        ClampGraphBuilder clampGraphBuilder = new ClampGraphBuilder(mockPainter);
        clampGraphBuilder.collector(collector).addMicroService(ms1).addMicroService(ms2).addPolicy(opPolicy).build();

        verify(mockPainter, times(1)).doPaint(collectorCaptor.capture(), microServicesCaptor.capture(),
                policyCaptor.capture());

        Assert.assertEquals(collector, collectorCaptor.getValue());
        Assert.assertEquals(microServices, microServicesCaptor.getValue());
        Assert.assertEquals(opPolicies, policyCaptor.getValue());
    }

    /**
     * Do a quick test of the graphBuilder chain when no policy is given.
     */
    @Test
    public void clampGraphBuilderNoPolicyGivenTest() {
        String collector = "VES";
        MicroServicePolicy ms1 =
                new MicroServicePolicy("ms1", new PolicyModel("org.onap.ms1", "", "1.0.0"), false, null);
        MicroServicePolicy ms2 =
                new MicroServicePolicy("ms2", new PolicyModel("org.onap.ms2", "", "1.0.0"), false, null);

        ClampGraphBuilder clampGraphBuilder = new ClampGraphBuilder(mockPainter);
        assertThat(clampGraphBuilder.collector(collector).addMicroService(ms1).addMicroService(ms2).build())
                .isNotNull();

    }
}
