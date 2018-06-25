/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 Nokia Intellectual Property. All rights
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

package org.onap.clamp.clds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.onap.clamp.clds.model.CldsInfo;


public class CldsInfoProviderTest {

    private static final String TEST_USERNAME = "TEST_USERNAME";

    @Test
    public void shouldProvideCldsInfoFromContext() throws Exception {

        // given
        CldsService serviceBase = mock(CldsService.class);
        when(serviceBase.getUserName()).thenReturn(TEST_USERNAME);
        when(serviceBase.isAuthorizedNoException(any())).thenReturn(true);
        CldsInfoProvider cldsInfoProvider = new CldsInfoProvider(serviceBase);

        // when
        CldsInfo cldsInfo = cldsInfoProvider.getCldsInfo();

        // then
        assertThat(cldsInfo.getUserName()).isEqualTo(TEST_USERNAME);
        assertThat(cldsInfo.isPermissionReadCl()).isTrue();
        assertThat(cldsInfo.isPermissionReadTemplate()).isTrue();
        assertThat(cldsInfo.isPermissionUpdateCl()).isTrue();
        assertThat(cldsInfo.isPermissionUpdateTemplate()).isTrue();
    }
}