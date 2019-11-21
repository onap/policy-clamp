/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.onap.clamp.loop.common.AuditEntity;

/**
 * Represents Dictionary.
 */

@Entity
@Table(name = "dictionary")
public class Dictionary extends AuditEntity implements Serializable {

    /**
     * The serial version id.
     */
    private static final long serialVersionUID = -286522707701388645L;

    @Id
    @Expose
    @Column(nullable = false, name = "name", unique = true)
    private String name;

    @Expose
    @Column(name = "dictionary_second_level")
    private int secondLevelDictionary;

    @Expose
    @Column(name = "dictionary_type")
    private String subDictionaryType;

    @Expose
    @OneToMany(mappedBy = "dictionary", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<DictionaryElement> dictionaryElements = new ArrayList<>();

    /**
     * name getter.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * name setter.
     * 
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * secondLevelDictionary getter.
     * 
     * @return the secondLevelDictionary
     */
    public int getSecondLevelDictionary() {
        return secondLevelDictionary;
    }

    /**
     * secondLevelDictionary setter.
     * 
     * @param secondLevelDictionary the secondLevelDictionary to set
     */
    public void setSecondLevelDictionary(int secondLevelDictionary) {
        this.secondLevelDictionary = secondLevelDictionary;
    }

    /**
     * subDictionaryType getter.
     * 
     * @return the subDictionaryType
     */
    public String getSubDictionaryType() {
        return subDictionaryType;
    }

    /**
     * subDictionaryType setter.
     * 
     * @param subDictionaryType the subDictionaryType to set
     */
    public void setSubDictionaryType(String subDictionaryType) {
        this.subDictionaryType = subDictionaryType;
    }

    /**
     * dictionaryElements getter.
     * 
     * @return the dictionaryElements
     */
    public List<DictionaryElement> getDictionaryElements() {
        return dictionaryElements;
    }

    /**
     * dictionaryElements setter.
     * 
     * @param dictionaryElements the dictionaryElements to set
     */
    public void setDictionaryElements(List<DictionaryElement> dictionaryElements) {
        this.dictionaryElements = dictionaryElements;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Dictionary other = (Dictionary) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
