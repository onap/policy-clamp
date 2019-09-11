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

package org.onap.clamp.flow;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.clamp.clds.util.OnapLogConstants;
import org.onap.clamp.flow.log.FlowLogOperation;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.test.util.ReflectionTestUtils;

public class FlowLogOperationTest {

    private FlowLogOperation flowLogOperation = new FlowLogOperation();

    @Test
    public void testStratLog() {
        // given
        LoggingUtils loggingUtils = mock(LoggingUtils.class);
        ReflectionTestUtils.setField(flowLogOperation, "util", loggingUtils);

        // when
        Mockito.when(loggingUtils.getProperties(OnapLogConstants.Mdcs.REQUEST_ID)).thenReturn("MockRequestId");
        Mockito.when(loggingUtils.getProperties(OnapLogConstants.Mdcs.INVOCATION_ID)).thenReturn("MockInvocationId");
        Mockito.when(loggingUtils.getProperties(OnapLogConstants.Mdcs.PARTNER_NAME)).thenReturn("MockPartnerName");
        Exchange exchange = new DefaultExchange(mock(CamelContext.class));
        flowLogOperation.startLog(exchange, "serviceName");

        // then
        assertThat(exchange.getProperty(OnapLogConstants.Headers.REQUEST_ID)).isEqualTo("MockRequestId");
        assertThat(exchange.getProperty(OnapLogConstants.Headers.INVOCATION_ID)).isEqualTo("MockInvocationId");
        assertThat(exchange.getProperty(OnapLogConstants.Headers.PARTNER_NAME)).isEqualTo("MockPartnerName");
    }

    @Test
    public void testInvokeLog() {
        // given
        final String mockEntity = "mockEntity";
        final String mockServiceName = "mockSerivceName";
        MDCAdapter mdcAdapter = MDC.getMDCAdapter();
        // when
        flowLogOperation.invokeLog(mockEntity, mockServiceName);
        // then
        String entity = mdcAdapter.get(OnapLogConstants.Mdcs.TARGET_ENTITY);
        String serviceName = mdcAdapter.get(OnapLogConstants.Mdcs.TARGET_SERVICE_NAME);
        assertEquals(entity, mockEntity);
        assertEquals(serviceName, mockServiceName);
    }

    @Test
    public void testEndLog() {
        // given
        MDC.put(OnapLogConstants.Mdcs.ENTRY_TIMESTAMP, "2019-05-19T00:00:00.007Z");
        MDCAdapter mdcAdapter = MDC.getMDCAdapter();
        /// when
        flowLogOperation.endLog();
        // then
        assertThat(mdcAdapter.get(OnapLogConstants.Mdcs.ENTRY_TIMESTAMP)).isNull();
    }

    @Test
    public void testErrorLog() {
        // given
        MDC.put(OnapLogConstants.Mdcs.ENTRY_TIMESTAMP, "2019-05-19T00:00:00.007Z");
        MDCAdapter mdcAdapter = MDC.getMDCAdapter();
        // when
        flowLogOperation.errorLog();
        // then
        assertThat(mdcAdapter.get(OnapLogConstants.Mdcs.ENTRY_TIMESTAMP)).isNull();
    }
}