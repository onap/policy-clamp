/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.policy.clamp.clds.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class RuntimeInstantiationResponseItTestCase {
    @Autowired
    CamelContext camelContext;

    private static final String DIRECT_GET_TOSCA_INSTANTIATION = "direct:get-tosca-instantiation";

    private static final String DIRECT_POST_TOSCA_INSTANCE_PROPERTIES = "direct:post-tosca-instance-properties";

    private static final String DIRECT_DELETE_TOSCA_INSTANCE_PROPERTIES = "direct:delete-tosca-instance-properties";

    private static final String DIRECT_PUT_TOSCA_INSTANCE_PROPERTIES = "direct:put-tosca-instantiation";

    private static final String SERVICE_TEMPLATE_NAME = "name";

    private static final String SERVICE_TEMPLATE_VERSION = "version";

    private static final String RAISE_HTTP_EXCEPTION_FLAG = "raiseHttpExceptionFlag";

    private static final String SAMPLE_CONTROL_LOOP_LIST = "{\"controlLoopList\": [{\"name\": \"PMSHInstance0\","
        + "\"version\": \"1.0.1\",\"definition\": {},\"state\": \"UNINITIALISED\",\"orderedState\": \"UNINITIALISED\","
        + "\"description\": \"PMSH control loop instance 0\",\"elements\": {}}]}";

    private static final String SAMPLE_TOSCA_TEMPLATE =
        "{\"tosca_definitions_version\": \"tosca_simple_yaml_1_1_0\","
            + "\"data_types\": {},\"node_types\": {}, \"policy_types\": {},"
            + " \"topology_template\": {},"
            + " \"name\": \"ToscaServiceTemplateSimple\", \"version\": \"1.0.0\", \"metadata\": {}}";

    private static final String SAMPLE_TOSCA_CHANGE_ORDER_STATE = "{\"orderedState\":\"PASSIVE\","
        + "\"controlLoopIdentifierList\":[{\"name\":\"PMSH_Instance1\",\"version\":\"2.3.1\"}]}";

    @Test
    public void testToscaServiceTemplateStatus() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send(DIRECT_GET_TOSCA_INSTANTIATION, ExchangeBuilder.anExchange(camelContext)
                .withProperty(SERVICE_TEMPLATE_NAME, "ToscaServiceTemplate")
                .withProperty(SERVICE_TEMPLATE_VERSION, "1.0.0")
                .withProperty(RAISE_HTTP_EXCEPTION_FLAG, "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testToscaInstantiationGetResponseBody() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send(DIRECT_GET_TOSCA_INSTANTIATION, ExchangeBuilder.anExchange(camelContext)
                .withProperty(SERVICE_TEMPLATE_NAME, "ToscaServiceTemplate")
                .withProperty(SERVICE_TEMPLATE_VERSION, "1.0.0")
                .withProperty(RAISE_HTTP_EXCEPTION_FLAG, "true")
                .build());

        assertThat(exchangeResponse.getIn().getBody()).hasToString(SAMPLE_CONTROL_LOOP_LIST);
    }

    @Test
    public void testToscaInstantiationStatus() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send(DIRECT_GET_TOSCA_INSTANTIATION, ExchangeBuilder.anExchange(camelContext)
                .withBody(SAMPLE_CONTROL_LOOP_LIST)
                .withProperty(RAISE_HTTP_EXCEPTION_FLAG, "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testCreateToscaInstancePropertiesStatus() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send(DIRECT_POST_TOSCA_INSTANCE_PROPERTIES, ExchangeBuilder.anExchange(camelContext)
                .withBody(SAMPLE_TOSCA_TEMPLATE)
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testDeleteToscaInstancePropertiesStatus() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send(DIRECT_DELETE_TOSCA_INSTANCE_PROPERTIES, ExchangeBuilder.anExchange(camelContext)
                .withProperty("name", "PMSH_Instance1")
                .withProperty("version", "2.3.1")
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testChangeOrderStateStatus() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send(DIRECT_PUT_TOSCA_INSTANCE_PROPERTIES, ExchangeBuilder.anExchange(camelContext)
                .withBody(SAMPLE_TOSCA_CHANGE_ORDER_STATE)
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }
}
