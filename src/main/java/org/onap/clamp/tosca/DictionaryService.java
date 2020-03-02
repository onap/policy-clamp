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

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DictionaryService {

    private final DictionaryRepository dictionaryRepository;
    private final DictionaryElementsRepository dictionaryElementsRepository;

    /**
     * Constructor.
     */
    @Autowired
    public DictionaryService(DictionaryRepository dictionaryRepository,
        DictionaryElementsRepository dictionaryElementsRepository) {
        this.dictionaryRepository = dictionaryRepository;
        this.dictionaryElementsRepository = dictionaryElementsRepository;

    }

    public Dictionary saveOrUpdateDictionary(Dictionary dictionary) {
        return dictionaryRepository.save(dictionary);
    }

    /**
     * Creates or Updates Dictionary Element.
     *
     * @param dictionaryName The Dictionary name
     * @param dictionary The Dictionary object with dictionary elements
     * @return updated Dictionary object with all dictionary elements
     */
    public Dictionary saveOrUpdateDictionaryElement(String dictionaryName, Dictionary dictionary) {
        Dictionary dict = getDictionary(dictionaryName);

        Set<DictionaryElement> newDictionaryElements = dictionary.getDictionaryElements();

        if (newDictionaryElements != null && !newDictionaryElements.isEmpty()) {
            Set<DictionaryElement> updatedDictionaryElements = newDictionaryElements.stream()
                .map(dictionaryElement -> getAndUpdateDictionaryElement(dict, dictionaryElement))
                .collect(Collectors.toSet());

            dict.getDictionaryElements().forEach(dictElement -> {
                if (!updatedDictionaryElements.contains(dictElement)) {
                    updatedDictionaryElements.add(dictElement);
                }
            });
            dict.setDictionaryElements(updatedDictionaryElements);
        }
        return dictionaryRepository.save(dict);

    }

    private DictionaryElement getAndUpdateDictionaryElement(Dictionary dictionary,
        DictionaryElement element) {
        return dictionaryElementsRepository
            .save(dictionaryElementsRepository.findById(element.getShortName())
                .map(p -> updateDictionaryElement(p, element, dictionary))
                .orElse(new DictionaryElement(element.getName(), element.getShortName(),
                    element.getDescription(), element.getType(), element.getSubDictionary(),
                    Sets.newHashSet(dictionary))));
    }

    public void deleteDictionary(Dictionary dictionary) {
        dictionaryRepository.delete(dictionary);
    }

    public void deleteDictionary(String dictionaryName) {
        dictionaryRepository.deleteById(dictionaryName);
    }

    public List<Dictionary> getAllDictionaries() {
        return dictionaryRepository.findAll();
    }

    public List<String> getAllSecondaryLevelDictionaryNames() {
        return dictionaryRepository.getAllSecondaryLevelDictionaryNames();
    }

    public Dictionary getDictionary(String dictionaryName) {
        return dictionaryRepository.findById(dictionaryName).orElseThrow(
            () -> new EntityNotFoundException("Couldn't find Dictionary named: " + dictionaryName));
    }

    /**
     * Deletes a dictionary element from Dictionary by shortName.
     *
     * @param dictionaryName The dictionary name
     * @param dictionaryElementShortName the dictionary Element Short name
     */
    public void deleteDictionaryElement(String dictionaryName, String dictionaryElementShortName) {
        if (dictionaryRepository.existsById(dictionaryName)) {
            DictionaryElement element =
                dictionaryElementsRepository.findById(dictionaryElementShortName).orElse(null);
            if (element != null) {
                Dictionary dict = getDictionary(dictionaryName);
                dict.removeDictionaryElement(element);
                dictionaryRepository.save(dict);
            }
        }
    }

    private DictionaryElement updateDictionaryElement(DictionaryElement oldDictionaryElement,
        DictionaryElement newDictionaryElement, Dictionary dictionary) {
        oldDictionaryElement.setName(newDictionaryElement.getName());
        oldDictionaryElement.setDescription(newDictionaryElement.getDescription());
        oldDictionaryElement.setType(newDictionaryElement.getType());
        oldDictionaryElement.setSubDictionary(newDictionaryElement.getSubDictionary());
        oldDictionaryElement.getUsedByDictionaries().add(dictionary);
        return oldDictionaryElement;
    }
}
