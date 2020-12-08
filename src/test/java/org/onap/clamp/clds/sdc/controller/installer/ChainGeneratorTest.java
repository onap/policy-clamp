/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
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

package org.onap.clamp.clds.sdc.controller.installer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class ChainGeneratorTest {
    private static final String FIRST_APPP = "first_app";
    private static final String SECOND_APPP = "second_app";
    private static final String THIRD_APPP = "third_app";
    private static final String FOURTH_APPP = "fourth_app";
    private static final String DEFAULT_VERSION = "1.0.0";

    @Test
    public void getChainOfMicroServicesTest() {
        BlueprintMicroService ms1 = new BlueprintMicroService(FIRST_APPP, "", "", DEFAULT_VERSION);
        BlueprintMicroService ms2 = new BlueprintMicroService(SECOND_APPP, "", FIRST_APPP, DEFAULT_VERSION);
        BlueprintMicroService ms3 = new BlueprintMicroService(THIRD_APPP, "", SECOND_APPP, DEFAULT_VERSION);
        BlueprintMicroService ms4 = new BlueprintMicroService(FOURTH_APPP, "", THIRD_APPP, DEFAULT_VERSION);

        List<BlueprintMicroService> expectedList = Arrays.asList(ms1, ms2, ms3, ms4);
        Set<BlueprintMicroService> inputSet = new HashSet<>(expectedList);

        List<BlueprintMicroService> actualList = new ChainGenerator().getChainOfMicroServices(inputSet);
        Assert.assertEquals(expectedList, actualList);
    }

    @Test
    public void getChainOfMicroServicesTwiceNoInputTest() {
        BlueprintMicroService ms1 = new BlueprintMicroService(FIRST_APPP, "", "", DEFAULT_VERSION);
        BlueprintMicroService ms2 = new BlueprintMicroService(SECOND_APPP, "", "", DEFAULT_VERSION);
        BlueprintMicroService ms3 = new BlueprintMicroService(THIRD_APPP, "", SECOND_APPP, DEFAULT_VERSION);
        BlueprintMicroService ms4 = new BlueprintMicroService(FOURTH_APPP, "", FIRST_APPP, DEFAULT_VERSION);

        Set<BlueprintMicroService> inputSet = new HashSet<>(Arrays.asList(ms1, ms2, ms3, ms4));
        List<BlueprintMicroService> actualList = new ChainGenerator().getChainOfMicroServices(inputSet);
        Assert.assertTrue(actualList.isEmpty());
    }

    @Test
    public void getChainOfMicroServicesBranchingTest() {
        BlueprintMicroService ms1 = new BlueprintMicroService(FIRST_APPP, "", "", DEFAULT_VERSION);
        BlueprintMicroService ms2 = new BlueprintMicroService(SECOND_APPP, "", FIRST_APPP, DEFAULT_VERSION);
        BlueprintMicroService ms3 = new BlueprintMicroService(THIRD_APPP, "", FIRST_APPP, DEFAULT_VERSION);
        BlueprintMicroService ms4 = new BlueprintMicroService(FOURTH_APPP, "", FIRST_APPP, DEFAULT_VERSION);

        Set<BlueprintMicroService> inputSet = new HashSet<>(Arrays.asList(ms1, ms2, ms3, ms4));
        List<BlueprintMicroService> actualList = new ChainGenerator().getChainOfMicroServices(inputSet);
        Assert.assertTrue(actualList.isEmpty());
    }
}
