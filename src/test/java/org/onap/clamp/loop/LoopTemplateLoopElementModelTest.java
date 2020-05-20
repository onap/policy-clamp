/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
 * ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

package org.onap.clamp.loop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.onap.clamp.loop.template.LoopElementModel;
import org.onap.clamp.loop.template.LoopTemplate;
import org.onap.clamp.loop.template.LoopTemplateLoopElementModel;
import org.onap.clamp.loop.template.PolicyModel;


public class LoopTemplateLoopElementModelTest {

    private LoopElementModel loopElementModel = getLoopElementModel("yaml", "microService1",
            getPolicyModel("org.onap.policy.drools", "yaml", "1.0.0", "Drools", "type1"));
    private LoopTemplate loopTemplate = getLoopTemplate("templateName", "yaml", 1);

    private LoopElementModel getLoopElementModel(String yaml, String name, PolicyModel policyModel) {
        LoopElementModel model = new LoopElementModel();
        model.setBlueprint(yaml);
        model.setName(name);
        model.addPolicyModel(policyModel);
        model.setLoopElementType("OPERATIONAL_POLICY");
        return model;
    }

    private PolicyModel getPolicyModel(String policyType, String policyModelTosca, String version, String policyAcronym,
                                       String policyVariant) {
        return new PolicyModel(policyType, policyModelTosca, version, policyAcronym);
    }

    private LoopTemplate getLoopTemplate(String name, String blueprint, Integer maxInstancesAllowed) {
        LoopTemplate template = new LoopTemplate(name, blueprint, maxInstancesAllowed, null);
        template.addLoopElementModel(loopElementModel);
        return template;
    }

    /**
     * This tests compareTo method.
     */
    @Test
    public void compareToTest() {
        LoopTemplateLoopElementModel model1 = new LoopTemplateLoopElementModel();
        LoopTemplateLoopElementModel model2 = new LoopTemplateLoopElementModel();
        assertThat(model1.compareTo(model2)).isEqualTo(1);

        model1.setFlowOrder(2);
        assertThat(model1.compareTo(model2)).isEqualTo(-1);

        model2.setFlowOrder(3);
        assertThat(model1.compareTo(model2)).isEqualTo(1);
    }

    /**
     * This tests equals method.
     */
    @Test
    public void equalsTest() {
        LoopTemplateLoopElementModel model1 = new LoopTemplateLoopElementModel();
        LoopTemplateLoopElementModel model2 = new LoopTemplateLoopElementModel();

        assertThat(model1.equals(model2)).isTrue();

        model1.setLoopTemplate(loopTemplate);
        assertThat(model1.equals(model2)).isFalse();
        model2.setLoopTemplate(loopTemplate);
        assertThat(model1.equals(model2)).isTrue();

        model1.setLoopElementModel(loopElementModel);
        assertThat(model1.equals(model2)).isFalse();
        model2.setLoopElementModel(loopElementModel);
        assertThat(model1.equals(model2)).isTrue();

        model1.setFlowOrder(1);
        assertThat(model1.equals(model2)).isTrue();
        model2.setFlowOrder(2);
        assertThat(model1.equals(model2)).isTrue();
    }

}
