/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

package org.onap.clamp.clds.tosca;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.onap.clamp.tosca.Dictionary;
import org.onap.clamp.tosca.DictionaryElement;
import org.onap.clamp.tosca.DictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class DictionaryRepositoriesTestItCase {
    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Test
    @Transactional
    public void crudTest() {
        // Setup
        Dictionary dictionaryTest1 = new Dictionary();
        dictionaryTest1.setName("testDictionary1");
        dictionaryTest1.setSecondLevelDictionary(1);
        dictionaryTest1.setSubDictionaryType("testType");

        DictionaryElement element1 = new DictionaryElement();
        element1.setName("element1");
        element1.setShortName("shortName1");
        element1.setType("type1");
        element1.setDescription("description1");

        dictionaryTest1.addDictionaryElements(element1);

        Dictionary dictionaryTest2 = new Dictionary();
        dictionaryTest2.setName("testDictionary2");
        dictionaryTest2.setSecondLevelDictionary(1);
        dictionaryTest2.setSubDictionaryType("testType");

        DictionaryElement element2 = new DictionaryElement();
        element2.setName("element2");
        element2.setShortName("shortName2");
        element2.setSubDictionary("testDictionary1");
        element2.setType("type2");
        element2.setDescription("description2");

        dictionaryTest2.addDictionaryElements(element2);

        dictionaryRepository.save(dictionaryTest1);
        List<String> res1 = dictionaryRepository.getAllDictionaryNames();
        assertThat(res1.size()).isGreaterThan(1);
        assertThat(res1).contains("testDictionary1");

        dictionaryRepository.save(dictionaryTest2);
        List<String> res2 = dictionaryRepository.getAllDictionaryNames();
        assertThat(res2.size()).isGreaterThan(2);
        assertThat(res2).contains("testDictionary1");
        assertThat(res2).contains("testDictionary2");
    }
}
