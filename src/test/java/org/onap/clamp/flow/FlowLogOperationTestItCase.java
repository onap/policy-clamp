package org.onap.clamp.flow;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.clamp.clds.util.ONAPLogConstants;
import org.onap.clamp.flow.log.FlowLogOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


public class FlowLogOperationTestItCase {

    @Autowired
    CamelContext camelContext;

    @Test
    public void testStratLog() {
        //given
        FlowLogOperation flowLogOperation = new FlowLogOperation();
        Exchange exchange = new DefaultExchange(camelContext);
        LoggingUtils loggingUtils = mock(LoggingUtils.class);
        ReflectionTestUtils.setField(flowLogOperation, "util", loggingUtils);

        //when
        Mockito.when(loggingUtils.getProperties(ONAPLogConstants.MDCs.REQUEST_ID)).thenReturn("MockRequestId");
        Mockito.when(loggingUtils.getProperties(ONAPLogConstants.MDCs.INVOCATION_ID)).thenReturn("MockInvocationId");
        Mockito.when(loggingUtils.getProperties(ONAPLogConstants.MDCs.PARTNER_NAME)).thenReturn("MockPartnerName");
        flowLogOperation.startLog(exchange, "serviceName");

        //then
        assertThat(exchange.getProperty(ONAPLogConstants.Headers.REQUEST_ID)).isEqualTo("MockRequestId");
        assertThat(exchange.getProperty(ONAPLogConstants.Headers.INVOCATION_ID)).isEqualTo("MockInvocationId");
        assertThat(exchange.getProperty(ONAPLogConstants.Headers.PARTNER_NAME)).isEqualTo("MockPartnerName");
    }
}