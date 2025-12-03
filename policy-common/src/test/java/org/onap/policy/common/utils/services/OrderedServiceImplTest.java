/*
 * ============LICENSE_START=======================================================
 * utils
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation
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

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderedServiceImplTest {

    private static final int HIGH_PRIORITY_NUM = -1000;
    private static final int LOW_PRIORITY_NUM = 1000;

    private static GenericService highPrioService;
    private static GenericService lowPrioService;

    /**
     * Saves the original state of the ordered service list to restore after each test.
     */
    @BeforeAll
    public static void setup() {
        List<GenericService> implementers = GenericService.providers.getList();
        highPrioService = implementers.get(0);
        lowPrioService = implementers.get(1);
    }

    /**
     * Restores original state after each test.
     */
    @BeforeEach
    public void resetOrder() {
        highPrioService.setSequenceNumber(HIGH_PRIORITY_NUM);
        lowPrioService.setSequenceNumber(LOW_PRIORITY_NUM);
    }

    /**
     * Tests obtaining a list of service implementers.
     */
    @Test
    void getListTest() {
        List<GenericService> implementers = GenericService.providers.getList();
        assertEquals(2, implementers.size());

        assertEquals(highPrioService, implementers.get(0));
        assertEquals(HIGH_PRIORITY_NUM, highPrioService.getSequenceNumber());

        assertEquals(lowPrioService, implementers.get(1));
        assertEquals(LOW_PRIORITY_NUM, lowPrioService.getSequenceNumber());
    }

    /**
     * Tests inverting the priority of two services to ensure the list is rebuilt
     * with the new order.
     */
    @Test
    void rebuildListInvertedPriorityTest() {

        List<GenericService> implementers = GenericService.providers.getList();
        assertEquals(2, implementers.size());

        assertEquals(highPrioService, implementers.get(0));
        assertEquals(HIGH_PRIORITY_NUM, highPrioService.getSequenceNumber());

        assertEquals(lowPrioService, implementers.get(1));
        assertEquals(LOW_PRIORITY_NUM, lowPrioService.getSequenceNumber());

        highPrioService.setSequenceNumber(LOW_PRIORITY_NUM);
        lowPrioService.setSequenceNumber(HIGH_PRIORITY_NUM);

        implementers = GenericService.providers.rebuildList();
        assertEquals(2, implementers.size());

        assertEquals(lowPrioService, implementers.get(0));
        assertEquals(HIGH_PRIORITY_NUM, lowPrioService.getSequenceNumber());

        assertEquals(highPrioService, implementers.get(1));
        assertEquals(LOW_PRIORITY_NUM, highPrioService.getSequenceNumber());

    }

    /**
     * Tests that the service list is ordered alphabetically by class names
     * if the priorities are equivalent.
     */
    @Test
    void rebuildListEqualPriorityTest() {

        List<GenericService> implementers = GenericService.providers.getList();
        assertEquals(2, implementers.size());

        assertEquals(highPrioService, implementers.get(0));
        assertEquals(HIGH_PRIORITY_NUM, highPrioService.getSequenceNumber());

        assertEquals(lowPrioService, implementers.get(1));
        assertEquals(LOW_PRIORITY_NUM, lowPrioService.getSequenceNumber());

        highPrioService.setSequenceNumber(LOW_PRIORITY_NUM);
        lowPrioService.setSequenceNumber(LOW_PRIORITY_NUM);

        implementers = GenericService.providers.rebuildList();
        assertEquals(2, implementers.size());

        assertEquals(highPrioService, implementers.get(0));
        assertEquals(LOW_PRIORITY_NUM, highPrioService.getSequenceNumber());

        assertEquals(lowPrioService, implementers.get(1));
        assertEquals(LOW_PRIORITY_NUM, lowPrioService.getSequenceNumber());

    }

    /**
     * Test interface that extends OrderedService to allow changing the sequence number.
     */
    public static interface GenericService extends OrderedService {

        /**
         * Providers of the GenericService interface.
         */
        OrderedServiceImpl<GenericService> providers = new OrderedServiceImpl<>(GenericService.class);

        /**
         * Sets the sequence number of the service.
         */
        public void setSequenceNumber(int seqNum);

    }

    /**
     * A high priority service class.
     */
    public static class HighPriorityService implements GenericService {

        /**
         * Defaults to a high priority.
         */
        private int seqNum = HIGH_PRIORITY_NUM;

        /**
         * {@inheritDoc}.
         */
        @Override
        public int getSequenceNumber() {
            return this.seqNum;
        }

        /**
         * {@inheritDoc}.
         */
        @Override
        public void setSequenceNumber(int seqNum) {
            this.seqNum = seqNum;
        }

    }

    /**
     * A low priority service class.
     */
    public static class LowPriorityService implements GenericService {

        /**
         * Defaults to a low priority.
         */
        private int seqNum = LOW_PRIORITY_NUM;

        /**
         * {@inheritDoc}.
         */
        @Override
        public int getSequenceNumber() {
            return this.seqNum;
        }

        /**
         * {@inheritDoc}.
         */
        @Override
        public void setSequenceNumber(int seqNum) {
            this.seqNum = seqNum;
        }

    }

}