/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.tosca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DictionaryServiceItCase {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryElementsRepository dictionaryElementsRepository;

    private DictionaryElement getDictionaryElement(String shortName, String name,
        String description, String type, String subDictionaryName) {

        return new DictionaryElement(name, shortName, description, type, subDictionaryName);

    }

    private Dictionary getSimpleDictionaryExample() {

        Dictionary dictionary = new Dictionary("Dictionary1", 0, null);

        dictionary.addDictionaryElements(getDictionaryElement("DE1", "DictionaryElement1",
            "DictionaryElement1", "string", null));

        dictionary.addDictionaryElements(getDictionaryElement("DE2", "DictionaryElement2",
            "DictionaryElement2", "number", null));

        return dictionary;
    }

    private Dictionary getSecondaryDictionaryExample() {

        Dictionary dictionary = new Dictionary("SecondaryDict", 1, "string");

        dictionary.addDictionaryElements(getDictionaryElement("SDE1", "SecondaryDictElement1",
            "SecondaryDictElement1", "string", null));

        dictionary.addDictionaryElements(getDictionaryElement("SDE2", "SecondaryDictElement2",
            "SecondaryDictElement2", "string", null));

        return dictionary;
    }

    /**
     * Test to validate that Dictionary is created.
     */
    @Test
    @Transactional
    public void shouldCreateDictionary() {
        Dictionary dictionary = getSimpleDictionaryExample();
        Dictionary actualDictionary = dictionaryService.saveOrUpdateDictionary(dictionary);
        assertNotNull(actualDictionary);
        assertThat(actualDictionary).isEqualTo(dictionary);
        assertThat(actualDictionary.getName()).isEqualTo(dictionary.getName());

        assertThat(actualDictionary.getDictionaryElements()).contains(
            dictionaryElementsRepository.findById("DE1").get(),
            dictionaryElementsRepository.findById("DE2").get());
    }

    /**
     * Test to validate a DictionaryElement is created for a Dictionary.
     */
    @Test
    @Transactional
    public void shouldCreateorUpdateDictionaryElement() {
        Dictionary dictionary = getSimpleDictionaryExample();
        Dictionary actualDictionary = dictionaryService.saveOrUpdateDictionary(dictionary);
        DictionaryElement dictionaryElement =
            getDictionaryElement("DictionaryElement3", "DE3", "DictionaryElement3", "date", null);
        actualDictionary.addDictionaryElements(dictionaryElement);
        Dictionary updatedDictionary = dictionaryService
            .saveOrUpdateDictionaryElement(actualDictionary.getName(), actualDictionary);
        assertNotNull(updatedDictionary);
        assertTrue(updatedDictionary.getDictionaryElements().contains(dictionaryElement));
        assertThat(updatedDictionary.getName()).isEqualTo(actualDictionary.getName());
        // update the dictionary element.
        dictionaryElement.setDescription("DictionaryElement3 New Description");
        Dictionary dictionary3 = new Dictionary("Dictionary1", 0, null);
        dictionary3.addDictionaryElements(dictionaryElement);
        Dictionary updatedDictionary2 =
            dictionaryService.saveOrUpdateDictionaryElement(dictionary3.getName(), dictionary3);

        assertNotNull(updatedDictionary2);
        assertTrue(updatedDictionary2.getDictionaryElements().contains(dictionaryElement));
        updatedDictionary2.getDictionaryElements().forEach(element -> {
            if (element.equals(dictionaryElement)) {
                assertTrue(element.getDescription().equals(dictionaryElement.getDescription()));
            }
        });

    }

    /**
     * Test to validate that All Dictionaries are retrieved.
     */
    @Test
    @Transactional
    public void shouldReturnAllDictionaries() {
        Dictionary dictionary = getSimpleDictionaryExample();
        Dictionary secondaryDictionary = getSecondaryDictionaryExample();
        dictionaryService.saveOrUpdateDictionary(dictionary);
        dictionaryService.saveOrUpdateDictionary(secondaryDictionary);

        List<Dictionary> list = dictionaryService.getAllDictionaries();
        assertNotNull(list);
        assertThat(list).contains(dictionary, secondaryDictionary);
    }

    /**
     * Test to validate one Dictionary is returned.
     */
    @Test
    @Transactional
    public void shouldReturnOneDictionary() {
        Dictionary dictionary = getSimpleDictionaryExample();
        dictionaryService.saveOrUpdateDictionary(dictionary);

        Dictionary returnedDictionary = dictionaryService.getDictionary("Dictionary1");
        assertNotNull(returnedDictionary);
        assertThat(returnedDictionary).isEqualTo(dictionary);
        assertThat(returnedDictionary.getDictionaryElements())
            .isEqualTo(dictionary.getDictionaryElements());
    }

    /**
     * Test to validate one Dictionary is returned.
     */
    @Test
    @Transactional
    public void shouldReturnEntityNotFoundException() {
        try {
            dictionaryService.getDictionary("Test");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(EntityNotFoundException.class);
            assertTrue(e.getMessage().equals("Couldn't find Dictionary named: Test"));
        }
    }

    /**
     * Test to validate Dictionary is deleted.
     */
    @Test
    @Transactional
    public void shouldDeleteDictionaryByObject() {
        Dictionary dictionary = getSimpleDictionaryExample();
        Dictionary returnedDictionary = dictionaryService.saveOrUpdateDictionary(dictionary);

        dictionaryService.deleteDictionary(returnedDictionary);
        try {
            dictionaryService.getDictionary("Dictionary1");
        } catch (EntityNotFoundException e) {
            assertTrue(e.getMessage().equals("Couldn't find Dictionary named: Dictionary1"));
        }
    }

    /**
     * Test to validate Dictionary is deleted by Name.
     */
    @Test
    @Transactional
    public void shouldDeleteDictionaryByName() {
        Dictionary dictionary = getSimpleDictionaryExample();
        dictionaryService.saveOrUpdateDictionary(dictionary);
        dictionaryService.deleteDictionary(dictionary.getName());
        try {
            dictionaryService.getDictionary("Dictionary1");
        } catch (EntityNotFoundException e) {
            assertTrue(e.getMessage().equals("Couldn't find Dictionary named: Dictionary1"));
        }
    }

    /**
     * Test to validate DictionaryElements is deleted by Name.
     */
    @Test
    @Transactional
    public void shouldDeleteDictionaryElementsByName() {
        Dictionary dictionary = getSimpleDictionaryExample();
        dictionaryService.saveOrUpdateDictionary(dictionary);
        DictionaryElement dictionaryElement =
            dictionaryElementsRepository.findById("DE1").orElse(null);
        assertNotNull(dictionaryElement);
        dictionaryService.deleteDictionaryElement("Dictionary1", "DE1");
        dictionary = dictionaryService.getDictionary("Dictionary1");
        DictionaryElement deletedDictionaryElement =
            dictionaryElementsRepository.findById("DE1").orElse(null);
        assertThat(deletedDictionaryElement).isNotIn(dictionary.getDictionaryElements());
    }

    /**
     * Test to validate all secondary level dictionary names are returned.
     */
    @Test
    @Transactional
    public void shouldReturnAllSecondaryLevelDictionaryNames() {
        Dictionary dictionary = getSecondaryDictionaryExample();
        dictionaryService.saveOrUpdateDictionary(dictionary);

        Dictionary dictionary2 = new Dictionary("SecondaryDict2", 1, "string");
        dictionaryService.saveOrUpdateDictionary(dictionary2);
        List<String> secondaryDictionaryNames =
            dictionaryService.getAllSecondaryLevelDictionaryNames();

        assertNotNull(secondaryDictionaryNames);
        assertThat(secondaryDictionaryNames).contains(dictionary.getName(), dictionary2.getName());
    }
}
