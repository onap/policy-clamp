/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.loop;

import static org.assertj.core.api.Assertions.assertThat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.clamp.clds.config.LegacyOperationalPolicyController;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.clamp.loop.components.external.ExternalComponentState;
import org.onap.clamp.loop.components.external.PolicyComponent;
import org.onap.clamp.loop.template.LoopTemplate;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;

public class PolicyComponentTest {

    /**
     * Test the computeState method.
     * oldState           newState        expectedFinalState
     * NOT_SENT      SENT_AND_DEPLOYED          NOT_SENT
     * NOT_SENT             SENT                NOT_SENT
     * NOT_SENT           NOT_SENT              NOT_SENT
     * NOT_SENT           IN_ERROR              IN_ERROR
     */
    @Test
    public void computeStateTestOriginalStateUnknown() {
        Exchange exchange = Mockito.mock(Exchange.class);
        Message message = Mockito.mock(Message.class);
        Exchange exchange2 = Mockito.mock(Exchange.class);
        Mockito.when(exchange.getIn()).thenReturn(message);
        Mockito.when(message.getExchange()).thenReturn(exchange2);
        // policy found + deployed
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(true);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(true);
        PolicyComponent policy = new PolicyComponent();

        ExternalComponentState state = policy.computeState(exchange);
        assertThat(state.getStateName()).isEqualTo("SENT_AND_DEPLOYED");
        // policy found + not deployed
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(true);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(false);
        ExternalComponentState state2 = policy.computeState(exchange);
        assertThat(state2.getStateName()).isEqualTo("SENT");
        // policy not found + not deployed
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(false);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(false);
        ExternalComponentState state4 = policy.computeState(exchange);
        assertThat(state4.getStateName()).isEqualTo("NOT_SENT");
        // policy not found + deployed
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(false);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(true);
        ExternalComponentState state3 = policy.computeState(exchange);
        assertThat(state3.getStateName()).isEqualTo("IN_ERROR");
    }

    /**
     * Test the computeState method.
     * oldState           newState        expectedFinalState
     * NOT_SENT      SENT_AND_DEPLOYED          NOT_SENT
     * NOT_SENT             SENT                NOT_SENT
     * NOT_SENT           NOT_SENT              NOT_SENT
     * NOT_SENT           IN_ERROR              IN_ERROR
     */
    @Test
    public void computeStateTestOriginalStateNotSent() {
        Exchange exchange = Mockito.mock(Exchange.class);
        Message message = Mockito.mock(Message.class);
        Exchange exchange2 = Mockito.mock(Exchange.class);
        Mockito.when(exchange.getIn()).thenReturn(message);
        Mockito.when(message.getExchange()).thenReturn(exchange2);
        // policy found + deployed
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(true);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(true);
        PolicyComponent policy = new PolicyComponent();
        ExternalComponentState notSent = new ExternalComponentState("NOT_SENT",
                "The policies defined have NOT yet been created on the policy engine", 90);
        policy.setState(notSent);
        ExternalComponentState state = policy.computeState(exchange);
        assertThat(state.getStateName()).isEqualTo("NOT_SENT");
        // policy found + not deployed
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(true);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(false);
        ExternalComponentState state2 = policy.computeState(exchange);
        assertThat(state2.getStateName()).isEqualTo("NOT_SENT");
        // policy not found + not deployed
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(false);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(false);
        ExternalComponentState state4 = policy.computeState(exchange);
        assertThat(state4.getStateName()).isEqualTo("NOT_SENT");
        // policy not found + deployed
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(false);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(true);
        ExternalComponentState state3 = policy.computeState(exchange);
        assertThat(state3.getStateName()).isEqualTo("IN_ERROR");
    }


    /**
     * Test the computeState method.
     * oldState           newState        expectedFinalState
     * SENT                 SENT                SENT
     * SENT          SENT_AND_DEPLOYED          SENT
     * SENT              IN_ERROR              IN_ERROR
     * SENT              NOT_SENT              NOT_SENT
     */
    @Test
    public void computeStateTestOriginalStateSent() throws IOException {
        Exchange exchange = Mockito.mock(Exchange.class);
        Message message = Mockito.mock(Message.class);
        Exchange exchange2 = Mockito.mock(Exchange.class);
        Mockito.when(exchange.getIn()).thenReturn(message);
        Mockito.when(message.getExchange()).thenReturn(exchange2);
        PolicyComponent policy = new PolicyComponent();
        ExternalComponentState sent = new ExternalComponentState("SENT",
                "The policies defined have been created but NOT deployed on the policy engine", 50);
        policy.setState(sent);
        // new policy state SENT
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(true);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(false);
        ExternalComponentState state = policy.computeState(exchange);
        assertThat(state.getStateName()).isEqualTo("SENT");
        // new policy state SENT_AND_DEPLOYED
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(true);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(true);
        ExternalComponentState state2 = policy.computeState(exchange);
        assertThat(state2.getStateName()).isEqualTo("SENT");
        // new policy state IN_ERROR
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(false);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(true);
        ExternalComponentState state3 = policy.computeState(exchange);
        assertThat(state3.getStateName()).isEqualTo("IN_ERROR");
        // new policy state NOT_SENT
        policy.setState(sent);
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(false);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(false);
        ExternalComponentState state4 = policy.computeState(exchange);
        assertThat(state4.getStateName()).isEqualTo("NOT_SENT");
    }

    /**
     * Test the computeState method.
     * oldState                   newState        expectedFinalState
     * SENT_AND_DEPLOYED     SENT_AND_DEPLOYED    SENT_AND_DEPLOYED
     * SENT_AND_DEPLOYED            SENT                SENT
     * SENT_AND_DEPLOYED          IN_ERROR            IN_ERROR
     * SENT_AND_DEPLOYED          NOT_SENT            NOT_SENT
     */
    @Test
    public void computeStateTestOriginalStateSentAndDeployed() throws IOException {
        Exchange exchange = Mockito.mock(Exchange.class);
        Message message = Mockito.mock(Message.class);
        Exchange exchange2 = Mockito.mock(Exchange.class);
        Mockito.when(exchange.getIn()).thenReturn(message);
        Mockito.when(message.getExchange()).thenReturn(exchange2);
        PolicyComponent policy = new PolicyComponent();
        ExternalComponentState sendDeployed = new ExternalComponentState("SENT_AND_DEPLOYED",
                "The policies defined have been created and deployed on the policy engine", 10);
        policy.setState(sendDeployed);
        // new policy state SENT_AND_DEPLOYED
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(true);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(true);
        ExternalComponentState state = policy.computeState(exchange);
        assertThat(state.getStateName()).isEqualTo("SENT_AND_DEPLOYED");
        // new policy state SENT
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(true);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(false);
        ExternalComponentState state2 = policy.computeState(exchange);
        assertThat(state2.getStateName()).isEqualTo("SENT");
        // new policy state IN_ERROR
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(false);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(true);
        ExternalComponentState state3 = policy.computeState(exchange);
        assertThat(state3.getStateName()).isEqualTo("IN_ERROR");
        // new policy state NOT_SENT
        policy.setState(sendDeployed);
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(false);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(false);
        ExternalComponentState state4 = policy.computeState(exchange);
        assertThat(state4.getStateName()).isEqualTo("NOT_SENT");
    }


    /**
     * Test the computeState method.
     * oldState           newState        expectedFinalState
     * IN_ERROR     SENT_AND_DEPLOYED         IN_ERROR
     * IN_ERROR            SENT               IN_ERROR
     * IN_ERROR          IN_ERROR             IN_ERROR
     * IN_ERROR          NOT_SENT             IN_ERROR
     */
    @Test
    public void computeStateTestOriginalStateInError() throws IOException {
        Exchange exchange = Mockito.mock(Exchange.class);
        Message message = Mockito.mock(Message.class);
        Exchange exchange2 = Mockito.mock(Exchange.class);
        Mockito.when(exchange.getIn()).thenReturn(message);
        Mockito.when(message.getExchange()).thenReturn(exchange2);
        PolicyComponent policy = new PolicyComponent();
        ExternalComponentState inError = new ExternalComponentState("IN_ERROR",
                "There was an error during the sending to policy, the policy engine may be corrupted or inconsistent",
                100);
        policy.setState(inError);
        // new policy state SENT_AND_DEPLOYED
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(true);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(true);
        ExternalComponentState state = policy.computeState(exchange);
        assertThat(state.getStateName()).isEqualTo("IN_ERROR");
        // new policy state SENT
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(true);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(false);
        ExternalComponentState state2 = policy.computeState(exchange);
        assertThat(state2.getStateName()).isEqualTo("IN_ERROR");
        // new policy state IN_ERROR
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(false);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(true);
        ExternalComponentState state3 = policy.computeState(exchange);
        assertThat(state3.getStateName()).isEqualTo("IN_ERROR");
        // new policy state NOT_SENT
        Mockito.when(exchange2.getProperty("policyFound")).thenReturn(false);
        Mockito.when(exchange2.getProperty("policyDeployed")).thenReturn(false);
        ExternalComponentState state4 = policy.computeState(exchange);

        assertThat(state4.getStateName()).isEqualTo("IN_ERROR");
    }

    /**
     * Test the create policies payload PdpGroup test.
     */
    @Test
    public void createPoliciesPayloadPdpGroupTest() throws IOException {
        Loop loopTest = new Loop("ControlLoopTest");
        PolicyModel policyModel1 = new PolicyModel("onap.policies.monitoring.test", null, "1.0.0");

        MicroServicePolicy microServicePolicy = new MicroServicePolicy("configPolicyTest", policyModel1, true,
                new Gson().fromJson("{\"configtype\":\"json\"}", JsonObject.class), null, "pdpGroup1", "pdpSubgroup1");
        loopTest.addMicroServicePolicy(microServicePolicy);

        MicroServicePolicy microServicePolicy2 = new MicroServicePolicy("configPolicyTest2", policyModel1, true,
                new Gson().fromJson("{\"configtype\":\"json\"}", JsonObject.class), null, "pdpGroup2", "pdpSubgroup1");
        loopTest.addMicroServicePolicy(microServicePolicy2);

        PolicyModel policyModel2 = new PolicyModel(LegacyOperationalPolicyController.OPERATIONAL_POLICY_LEGACY, null,
                "1.0.0");
        OperationalPolicy opPolicy =
                new OperationalPolicy("opPolicy", new Gson().fromJson("{\"configtype\":\"json\"}", JsonObject.class),
                        new Gson().fromJson("{\"jsonschema\":\"schema\"}", JsonObject.class), policyModel2, null,
                        "pdpGroup2",
                        "pdpSubgroup2");

        loopTest.addOperationalPolicy(opPolicy);
        OperationalPolicy opLegacyPolicy =
                new OperationalPolicy("opLegacyPolicy", new Gson().fromJson(
                        "{\"guard_policies\":[{\"policy-id\":\"guard1\"}]}", JsonObject.class),
                        new Gson().fromJson("{\"jsonschema\":\"schema\"}", JsonObject.class), policyModel2, null,
                        "pdpGroup2",
                        "pdpSubgroup2");

        loopTest.addOperationalPolicy(opLegacyPolicy);

        LoopTemplate loopTemplate = new LoopTemplate("test", "yaml", 1, null);
        loopTemplate.setDcaeBlueprintId("UUID-blueprint");
        loopTest.setLoopTemplate(loopTemplate);

        String payload = PolicyComponent.createPoliciesPayloadPdpGroup(loopTest, "POST");
        String expectedRes = ResourceFileUtil.getResourceAsString("tosca/pdp-group-policy-payload.json");

        assertThat(payload).isEqualTo(expectedRes);
    }
}
