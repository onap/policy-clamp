/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
public class RuntimeCommissioningResponseItTestCase {
    @Autowired
    CamelContext camelContext;

    private static final String SAMPLE_TOSCA_TEMPLATE =
        "{\"tosca_definitions_version\": \"tosca_simple_yaml_1_1_0\","
            + "\"data_types\": {},\"node_types\": {}, \"policy_types\": {},"
            + " \"topology_template\": {},"
            + " \"name\": \"ToscaServiceTemplateSimple\", \"version\": \"1.0.0\", \"metadata\": {}}";

    @Test
    public void testToscaServiceTemplateSchemaStatus() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send("direct:get-json-schema", ExchangeBuilder.anExchange(camelContext)
                .withProperty("section", "data_types")
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testToscaServiceTemplateStatus() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send("direct:get-service-template", ExchangeBuilder.anExchange(camelContext)
                .withProperty("name", "ToscaServiceTemplate")
                .withProperty("version", "1.0.0")
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testToscaServiceTemplateGetResponseBody() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send("direct:get-service-template", ExchangeBuilder.anExchange(camelContext)
                .withProperty("name", "ToscaServiceTemplate")
                .withProperty("version", "1.0.0")
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(exchangeResponse.getIn().getBody()).hasToString(SAMPLE_TOSCA_TEMPLATE);
    }

    @Test
    public void testCommissioningOfToscaServiceTemplateStatus() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send("direct:commission-service-template", ExchangeBuilder.anExchange(camelContext)
                .withBody(SAMPLE_TOSCA_TEMPLATE)
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testDecommissioningOfToscaServiceTemplateStatus() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send("direct:decommission-service-template", ExchangeBuilder.anExchange(camelContext)
                .withProperty("name", "ToscaServiceTemplate")
                .withProperty("version", "1.0.0")
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testGetControlLoopDefinitions() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send("direct:get-acm-definitions", ExchangeBuilder.anExchange(camelContext)
                .withProperty("name", "ToscaServiceTemplate")
                .withProperty("version", "1.0.0")
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testGetControlLoopElementDefinitions() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send("direct:get-element-definitions", ExchangeBuilder.anExchange(camelContext)
                .withProperty("name", "ToscaServiceTemplate")
                .withProperty("version", "1.0.0")
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testGetCommonOrInstancePropertiesCommonTrue() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send("direct:get-common-or-instance-properties", ExchangeBuilder.anExchange(camelContext)
                .withProperty("name", "ToscaServiceTemplate")
                .withProperty("version", "1.0.0")
                .withProperty("common", true)
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testGetCommonOrInstancePropertiesCommonFalse() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send("direct:get-common-or-instance-properties", ExchangeBuilder.anExchange(camelContext)
                .withProperty("name", "ToscaServiceTemplate")
                .withProperty("version", "1.0.0")
                .withProperty("common", false)
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }

    @Test
    public void testGetCommonOrInstancePropertiesCommonMissing() {
        ProducerTemplate prodTemplate = camelContext.createProducerTemplate();

        Exchange exchangeResponse =
            prodTemplate.send("direct:get-common-or-instance-properties", ExchangeBuilder.anExchange(camelContext)
                .withProperty("name", "ToscaServiceTemplate")
                .withProperty("version", "1.0.0")
                .withProperty("raiseHttpExceptionFlag", "true")
                .build());

        assertThat(HttpStatus.valueOf((Integer) exchangeResponse.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE))
            .is2xxSuccessful()).isTrue();
    }
}
