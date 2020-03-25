/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications copyright (c) 2019 Nokia
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
 * ===================================================================
 *
 */

package org.onap.clamp.loop.deploy;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.LinkedHashSet;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.clamp.loop.Loop;
import org.onap.clamp.loop.template.LoopElementModel;
import org.onap.clamp.loop.template.LoopTemplate;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.sdc.tosca.parser.exceptions.SdcToscaParserException;

public class BlueprintInputParametersTest {

    /**
     * getDeploymentParametersinJsonMultiBlueprintsTest.
     *
     * @throws IOException             in case of failure
     * @throws SdcToscaParserException in case of failure
     */
    @Test
    public void getDeploymentParametersinJsonMultiBlueprintsTest() throws IOException, SdcToscaParserException {


        MicroServicePolicy umService1 = Mockito.mock(MicroServicePolicy.class);
        Mockito.when(umService1.getName()).thenReturn("testName1");

        LoopElementModel loopElement = Mockito.mock(LoopElementModel.class);
        String blueprint1 = ResourceFileUtil.getResourceAsString("example/sdc/blueprint-dcae/tca.yaml");
        Mockito.when(loopElement.getBlueprint()).thenReturn(blueprint1);
        Mockito.when(umService1.getLoopElementModel()).thenReturn(loopElement);

        MicroServicePolicy umService2 = Mockito.mock(MicroServicePolicy.class);
        Mockito.when(umService2.getName()).thenReturn("testName2");

        LoopElementModel loopElement2 = Mockito.mock(LoopElementModel.class);
        String blueprint2 = ResourceFileUtil.getResourceAsString("example/sdc/blueprint-dcae/tca_2.yaml");
        Mockito.when(loopElement2.getBlueprint()).thenReturn(blueprint2);
        Mockito.when(umService2.getLoopElementModel()).thenReturn(loopElement2);

        MicroServicePolicy umService3 = Mockito.mock(MicroServicePolicy.class);
        Mockito.when(umService3.getName()).thenReturn("testName3");

        LoopElementModel loopElement3 = Mockito.mock(LoopElementModel.class);
        String blueprint3 = ResourceFileUtil.getResourceAsString("example/sdc/blueprint-dcae/tca_3.yaml");
        Mockito.when(loopElement3.getBlueprint()).thenReturn(blueprint3);
        Mockito.when(umService3.getLoopElementModel()).thenReturn(loopElement3);

        LinkedHashSet<MicroServicePolicy> umServiceSet = new LinkedHashSet<>();
        umServiceSet.add(umService1);
        umServiceSet.add(umService2);
        umServiceSet.add(umService3);
        Loop loop = Mockito.mock(Loop.class);
        Mockito.when(loop.getMicroServicePolicies()).thenReturn(umServiceSet);

        LoopTemplate template = Mockito.mock(LoopTemplate.class);
        Mockito.when(template.getUniqueBlueprint()).thenReturn(false);
        Mockito.when(loop.getLoopTemplate()).thenReturn(template);

        JsonObject paramJson = DcaeDeployParameters.getDcaeDeploymentParametersInJson(loop);

        Assert.assertEquals(JsonUtils.GSON_JPA_MODEL.toJson(paramJson),
                ResourceFileUtil.getResourceAsString(
                        "example/sdc/expected-result/deployment-parameters-multi-blueprints.json"));
    }

    /**
     * getDeploymentParametersInJsonSingleBlueprintTest.
     *
     * @throws IOException In case of failure
     * @throws SdcToscaParserException In case of failure
     */
    @Test
    public void getDeploymentParametersInJsonSingleBlueprintTest() throws IOException, SdcToscaParserException {
        Loop loop = Mockito.mock(Loop.class);

        MicroServicePolicy umService1 = Mockito.mock(MicroServicePolicy.class);
        Mockito.when(umService1.getName()).thenReturn("testName1");
        LinkedHashSet<MicroServicePolicy> umServiceSet = new LinkedHashSet<MicroServicePolicy>();
        umServiceSet.add(umService1);
        Mockito.when(loop.getMicroServicePolicies()).thenReturn(umServiceSet);

        LoopTemplate template = Mockito.mock(LoopTemplate.class);
        Mockito.when(template.getUniqueBlueprint()).thenReturn(true);
        String blueprint = ResourceFileUtil.getResourceAsString("example/sdc/blueprint-dcae/tca.yaml");
        Mockito.when(template.getBlueprint()).thenReturn(blueprint);
        Mockito.when(loop.getLoopTemplate()).thenReturn(template);

        JsonObject paramJson = DcaeDeployParameters.getDcaeDeploymentParametersInJson(loop);

        Assert.assertEquals(JsonUtils.GSON_JPA_MODEL.toJson(paramJson),
                ResourceFileUtil.getResourceAsString(
                        "example/sdc/expected-result/deployment-parameters-single-blueprint.json"));
    }
}
