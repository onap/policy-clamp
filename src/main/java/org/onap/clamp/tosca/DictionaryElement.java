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

import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import org.onap.clamp.loop.common.AuditEntity;

/**
 * Represents a Dictionary Item.
 */
@Entity
@Table(name = "dictionary_elements")
public class DictionaryElement extends AuditEntity implements Serializable {

    /**
     * The serial version id.
     */
    private static final long serialVersionUID = -286522707701388644L;

    @Id
    @Expose
    @Column(nullable = false, name = "short_name")
    private String shortName;

    @Expose
    @Column(nullable = false, name = "name")
    private String name;

    @Expose
    @Column(nullable = false, name = "description")
    private String description;

    @Expose
    @Column(nullable = false, name = "type")
    private String type;

    @Expose
    @Column(nullable = true, name = "subdictionary_name")
    private String subDictionary;

    @ManyToMany(mappedBy = "dictionaryElements", fetch = FetchType.EAGER)
    private Set<Dictionary> usedByDictionaries = new HashSet<>();

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
     * shortName getter.
     *
     * @return the shortName
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * shortName setter.
     *
     * @param shortName the shortName to set
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    /**
     * description getter.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * description setter.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * type getter.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * type setter.
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * subDictionary getter.
     *
     * @return the subDictionary
     */
    public String getSubDictionary() {
        return subDictionary;
    }

    /**
     * subDictionary setter.
     *
     * @param subDictionary the subDictionary to set
     */
    public void setSubDictionary(String subDictionary) {
        this.subDictionary = subDictionary;
    }

    /**
     * usedByDictionaries getter.
     *
     * @return the usedByDictionaries
     */
    public Set<Dictionary> getUsedByDictionaries() {
        return usedByDictionaries;
    }

    /**
     * usedByDictionaries setter.
     *
     * @param usedByDictionaries the usedByDictionaries to set
     */
    public void setUsedByDictionaries(Set<Dictionary> usedByDictionaries) {
        this.usedByDictionaries = usedByDictionaries;
    }

    /**
     * Default Constructor.
     */
    public DictionaryElement() {
    }

    /**
     * Constructor.
     *
     * @param name The Dictionary element name
     * @param shortName The short name
     * @param description The description
     * @param type The type of element
     * @param subDictionary The sub type
     */
    public DictionaryElement(String name, String shortName, String description, String type,
        String subDictionary) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.type = type;
        this.subDictionary = subDictionary;
    }

    /**
     * Constructor.
     *
     * @param name The Dictionary element name
     * @param shortName The short name
     * @param description The description
     * @param type The type of element
     * @param subDictionary The sub type
     */
    public DictionaryElement(String name, String shortName, String description, String type,
        String subDictionary, Set<Dictionary> usedByDictionaries) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.type = type;
        this.subDictionary = subDictionary;
        this.usedByDictionaries = usedByDictionaries;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((shortName == null) ? 0 : shortName.hashCode());
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
        DictionaryElement other = (DictionaryElement) obj;
        if (shortName == null) {
            if (other.shortName != null) {
                return false;
            }
        } else if (!shortName.equals(other.shortName)) {
            return false;
        }
        return true;
    }
}
