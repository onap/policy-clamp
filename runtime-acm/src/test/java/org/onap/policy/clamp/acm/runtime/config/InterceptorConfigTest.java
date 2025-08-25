/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.main.utils.EndPointInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class InterceptorConfigTest {

    @Test
    public void testGetWebMvcConfigurerAddsInterceptor() {
        InterceptorConfig config = new InterceptorConfig();
        EndPointInterceptor interceptor = mock(EndPointInterceptor.class);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);

        when(registry.addInterceptor(interceptor)).thenReturn(null);

        WebMvcConfigurer webMvcConfigurer = config.getWebMvcConfigurer(interceptor);
        webMvcConfigurer.addInterceptors(registry);

        verify(registry, times(1)).addInterceptor(interceptor);
    }
}
