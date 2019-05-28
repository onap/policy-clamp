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

package org.onap.clamp.clds.client;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.camel.Exchange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.clamp.clds.dao.CldsDao;

@RunWith(MockitoJUnitRunner.class)
public class CldsEventDelegateTest {

    private static final String CONTROL_NAME_KEY = "controlName";
    private static final String TEST_KEY = "isTest";
    private static final String INSERT_TEST_EVENT_KEY = "isInsertTestEvent";
    private static final String PREFIX = "abcdef-";
    private static final String UUID = "ABCDEFGHIJKLMNOPQRSTUVWXYZ-123456789";

    @Mock
    private Exchange exchange;

    @Mock
    private CldsDao cldsDao;

    @InjectMocks
    private CldsEventDelegate cldsEventDelegate;

    @Test
    public void shouldExecuteSuccessfully() {
        // given
        when(exchange.getProperty(eq(CONTROL_NAME_KEY))).thenReturn(PREFIX + UUID);
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(false);
        when(exchange.getProperty(eq(INSERT_TEST_EVENT_KEY))).thenReturn(false);

        // when
        cldsEventDelegate.addEvent(exchange, null);

        // then
        verify(cldsDao).insEvent(eq(null), eq(PREFIX), eq(UUID), any());
    }

    @Test
    public void shouldExecuteWithoutInsertingEventIntoDatabase() {
        // given
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(true);
        when(exchange.getProperty(eq(INSERT_TEST_EVENT_KEY))).thenReturn(false);

        // when
        cldsEventDelegate.addEvent(exchange, null);

        // then
        verify(cldsDao, never()).insEvent(any(), any(), any(), any());
    }
}