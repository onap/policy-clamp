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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
    @Column(nullable = false, name = "name", unique = true)
    private String name;

    @Expose
    @Column(nullable = false, name = "short_name", unique = true)
    private String shortName;

    @Expose
    @Column(name = "description")
    private String description;

    @Expose
    @Column(nullable = false, name = "type")
    private String type;

    @Column(name = "subdictionary_id", nullable = false)
    @Expose
    private String subDictionary;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "dictionary_id")
    private Dictionary dictionary;

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
     * dictionary getter.
     * 
     * @return the dictionary
     */
    public Dictionary getDictionary() {
        return dictionary;
    }

    /**
     * dictionary setter.
     * 
     * @param dictionary the dictionary to set
     */
    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * Default Constructor.
     */
    public DictionaryElement() {
    }

    /**
     * Constructor.
     * 
     * @param name          The Dictionary element name
     * @param shortName     The short name
     * @param description   The description
     * @param type          The type of element
     * @param subDictionary The sub type
     * @param dictionary    The parent dictionary
     */
    public DictionaryElement(String name, String shortName, String description, String type, String subDictionary,
            Dictionary dictionary) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.type = type;
        this.subDictionary = subDictionary;
        this.dictionary = dictionary;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dictionary == null) ? 0 : dictionary.hashCode());
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
        DictionaryElement other = (DictionaryElement) obj;
        if (dictionary == null) {
            if (other.dictionary != null) {
                return false;
            }
        } else if (!dictionary.equals(other.dictionary)) {
            return false;
        }
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
