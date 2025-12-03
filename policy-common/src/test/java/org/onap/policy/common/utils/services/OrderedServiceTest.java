/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.utils.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OrderedServiceTest {

    @Test
    void testGetSequenceNumber() {
        // Anonymous class implementation for testing
        OrderedService service = () -> 5;  // Returns 5 as the sequence number

        // Test getSequenceNumber
        assertEquals(5, service.getSequenceNumber(), "The sequence number should be 5");
    }

    @Test
    void testGetName() {
        // Anonymous class implementation for testing
        OrderedService service = () -> 5;

        // Test getName
        assertEquals(service.getClass().getName(), service.getName(), "The name should match the class name");
    }

    @Test
    void testGetNameWithCustomImplementation() {
        // Custom implementation of OrderedService
        class CustomOrderedService implements OrderedService {
            @Override
            public int getSequenceNumber() {
                return 10;
            }
        }

        OrderedService service = new CustomOrderedService();

        // Test getName for custom implementation
        assertEquals(service.getClass().getName(), service.getName(),
            "The name should match the custom implementation class name");
    }
}

