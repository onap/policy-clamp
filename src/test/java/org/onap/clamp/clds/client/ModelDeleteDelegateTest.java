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

import static org.mockito.Mockito.eq;
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
public class ModelDeleteDelegateTest {

    private static final String NAME_KEY = "modelName";
    private static final String NAME_VALUE = "model.name";

    @Mock
    private Exchange exchange;

    @Mock
    private CldsDao cldsDao;

    @InjectMocks
    private ModelDeleteDelegate modelDeleteDelegate;

    @Test
    public void shouldExecuteSuccessfully() {
        // given
        when(exchange.getProperty(eq(NAME_KEY))).thenReturn(NAME_VALUE);

        // when
        modelDeleteDelegate.execute(exchange);

        // then
        verify(cldsDao).deleteModel(eq(NAME_VALUE));
    }
}
