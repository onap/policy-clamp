/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.document.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaEntity;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.base.PfNameVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaEntity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocUtil {

    public static final String REF_DATA_TYPES = "dataTypes";
    public static final String REF_POLICY_TYPES = "policyTypes";
    public static final String REF_NODE_TYPES = "nodeTypes";
    public static final String REF_CAPABILITY_TYPES = "capabilityTypes";
    public static final String REF_RELATIONSHIP_TYPES = "relationshipTypes";
    public static final String REF_NODE_TEMPLATES = "nodeTemplates";
    public static final String REF_POLICIES = "policies";
    public static final String REF_REQUIREMENTS = "requirements";
    public static final String REF_CAPABILITIES = "capabilities";

    /**
     * Convenience method to apply a mapping function to all of the values of a map, generating a new map.
     *
     * @param source map whose values are to be mapped, or {@code null}
     * @param mapFunc mapping function
     * @return a new map, containing mappings of all of the items in the original map
     */
    public static <A extends PfNameVersion, R> Map<String, R> docMapToMap(Map<String, A> source,
            Function<A, R> mapFunc) {
        return docMapToMap(source, mapFunc, null);
    }

    /**
     * Convenience method to apply a mapping function to all of the values of a map, generating a new map.
     *
     * @param source map whose values are to be mapped, or {@code null}
     * @param mapFunc mapping function
     * @param defaultValue if source is null
     * @return a new map, containing mappings of all of the items in the original map
     */
    public static <A extends PfNameVersion, R> Map<String, R> docMapToMap(Map<String, A> source, Function<A, R> mapFunc,
            Map<String, R> defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        Map<String, R> map = new LinkedHashMap<>();
        for (Entry<String, A> ent : source.entrySet()) {
            map.put(ent.getValue().getName(), mapFunc.apply(ent.getValue()));
        }

        return map;
    }

    /**
     * Convenience method to apply a mapping function to all of the values of a map, generating a new map.
     *
     * @param source map whose values are to be mapped, or {@code null}
     * @param mapFunc mapping function
     * @return a new map, containing mappings of all of the items in the original map, or {@code null} if the source is
     *         {@code null}
     */
    public static <A extends ToscaEntity, R> Map<String, R> mapToDocMap(Map<String, A> source, Function<A, R> mapFunc) {
        return mapToDocMap(source, mapFunc, null);
    }

    /**
     * Convenience method to apply a mapping function to all of the values of a map, generating a new map.
     *
     * @param source map whose values are to be mapped, or {@code null}
     * @param mapFunc mapping function
     * @param defaultValue if source is null
     * @return a new map, containing mappings of all of the items in the original map, or defaultValue if the source is
     *         {@code null}
     */
    public static <A extends ToscaEntity, R> Map<String, R> mapToDocMap(Map<String, A> source, Function<A, R> mapFunc,
            Map<String, R> defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        Map<String, R> conceptMap = new LinkedHashMap<>();

        for (var incomingConceptEntry : source.entrySet()) {

            var conceptKey = new PfConceptKey();
            conceptKey.setName(incomingConceptEntry.getKey());
            if (incomingConceptEntry.getValue().getVersion() != null) {
                conceptKey.setVersion(incomingConceptEntry.getValue().getVersion());
            }

            incomingConceptEntry.getValue().setName(findConceptField(conceptKey, conceptKey.getName(),
                    incomingConceptEntry.getValue(), PfNameVersion::getDefinedName));
            incomingConceptEntry.getValue().setVersion(findConceptField(conceptKey, conceptKey.getVersion(),
                    incomingConceptEntry.getValue(), PfNameVersion::getDefinedVersion));

            var authoritiveImpl = mapFunc.apply(incomingConceptEntry.getValue());

            // After all that, save the map entry
            conceptMap.put(conceptKey.getId(), authoritiveImpl);
        }

        return conceptMap;
    }

    private static String findConceptField(final PfConceptKey conceptKey, final String keyFieldValue,
            final PfNameVersion concept, final Function<PfNameVersion, String> fieldGetterFunction) {

        String conceptField = fieldGetterFunction.apply(concept);

        if (StringUtils.isBlank(conceptField) || keyFieldValue.equals(conceptField)) {
            return keyFieldValue;
        } else {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, "Key " + conceptKey.getId() + " field "
                    + keyFieldValue + " does not match the value " + conceptField + " in the concept field");
        }
    }

    /**
     * Convenience method to apply a mapping function to all of the values of a list of maps, generating a new map.
     *
     * @param source list of maps whose values are to be mapped, or {@code null}
     * @param mapFunc mapping function
     * @return a new map, containing mappings of all of the items in the original map, or {@code null} if the source is
     *         {@code null}
     */
    public static <A extends ToscaEntity, R> Map<String, R> listToDocMap(List<Map<String, A>> source,
            Function<A, R> mapFunc) {

        return listToDocMap(source, mapFunc, null);
    }

    /**
     * Convenience method to apply a mapping function to all of the values of a list of maps, generating a new map.
     *
     * @param source list of maps whose values are to be mapped, or {@code null}
     * @param mapFunc mapping function
     * @param defaultValue if source is null
     * @return a new map, containing mappings of all of the items in the original map, or defaultValue if the source is
     *         {@code null}
     */
    public static <A extends ToscaEntity, R> Map<String, R> listToDocMap(List<Map<String, A>> source,
            Function<A, R> mapFunc, Map<String, R> defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        Map<String, R> conceptMap = new LinkedHashMap<>();

        for (var map : source) {
            conceptMap.putAll(mapToDocMap(map, mapFunc));
        }

        return conceptMap;
    }

    /**
     * Convenience method to apply a mapping function to all of the values of a map, generating a new list of maps.
     *
     * @param source map whose values are to be mapped, or {@code null}
     * @param mapFunc mapping function
     * @return a new list of maps, containing mappings of all of the items in the original map, or {@code null} if the
     *         source is {@code null}
     */
    public static <A extends PfNameVersion, R> List<Map<String, R>> docMapToList(Map<String, A> source,
            Function<A, R> mapFunc) {
        return docMapToList(source, mapFunc, null);
    }

    /**
     * Convenience method to apply a mapping function to all of the values of a map, generating a new list of maps.
     *
     * @param source map whose values are to be mapped, or {@code null}
     * @param mapFunc mapping function
     * @param defaultValue if source is null
     * @return a new list of maps, containing mappings of all of the items in the original map, or defaultValue if the
     *         source is {@code null}
     */
    public static <A extends PfNameVersion, R> List<Map<String, R>> docMapToList(Map<String, A> source,
            Function<A, R> mapFunc, List<Map<String, R>> defaultValue) {
        if (source == null) {
            return defaultValue;
        }

        List<Map<String, R>> result = new ArrayList<>();
        for (Entry<String, A> ent : source.entrySet()) {
            Map<String, R> map = new LinkedHashMap<>();
            map.put(ent.getValue().getName(), mapFunc.apply(ent.getValue()));
            result.add(map);

        }

        return result;
    }

    /**
     * Create DocConceptKey.
     *
     * @param name the name
     * @param version the version
     * @return DocConceptKey
     */
    public static DocConceptKey createDocConceptKey(String name, String version) {
        var key = new DocConceptKey();
        if (version != null && !version.isBlank()) {
            key.setName(name);
            key.setVersion(version);
        } else {
            var list = name.split(":");
            switch (list.length) {
                case 0:
                case 1:
                    key.setName(name);
                    key.setVersion(PfKey.NULL_KEY_VERSION);
                    break;
                case 2:
                    key.setName(list[0]);
                    key.setVersion(list[1]);
                    break;
                default:
            }
        }
        return key;
    }

    /**
     * Get DocToscaReferences.
     *
     * @return ToscaReferenceType
     */
    public static Map<String, Set<String>> getToscaReferences(DocToscaServiceTemplate serviceTemplate) {
        var referenceType = new HashMap<String, Set<String>>();
        fillReferenceType(referenceType, REF_DATA_TYPES, serviceTemplate.getDataTypes());
        fillReferenceType(referenceType, REF_POLICY_TYPES, serviceTemplate.getPolicyTypes());
        fillReferenceType(referenceType, REF_NODE_TYPES, serviceTemplate.getNodeTypes());
        fillReferenceType(referenceType, REF_CAPABILITY_TYPES, serviceTemplate.getCapabilityTypes());
        fillReferenceType(referenceType, REF_RELATIONSHIP_TYPES, serviceTemplate.getRelationshipTypes());

        if (serviceTemplate.getNodeTypes() != null) {

            serviceTemplate.getNodeTypes().values().forEach(
                    nodeType -> fillReferenceType(referenceType, REF_REQUIREMENTS, nodeType.getRequirements()));
        }
        if (serviceTemplate.getToscaTopologyTemplate() != null) {

            fillReferenceType(referenceType, REF_NODE_TEMPLATES,
                    serviceTemplate.getToscaTopologyTemplate().getNodeTemplates());

            fillReferenceType(referenceType, REF_POLICIES, serviceTemplate.getToscaTopologyTemplate().getPolicies());

            if (serviceTemplate.getToscaTopologyTemplate().getNodeTemplates() != null) {
                for (var nodeTemplate : serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().values()) {
                    fillReferenceType(referenceType, REF_REQUIREMENTS, nodeTemplate.getRequirements());
                    fillReferenceType(referenceType, REF_CAPABILITIES, nodeTemplate.getCapabilities());
                }
            }

        }
        return referenceType;
    }

    private static <A extends DocToscaEntity<?>> void fillReferenceType(Map<String, Set<String>> referenceType,
            String type, Map<String, A> map) {
        referenceType.putIfAbsent(type, new HashSet<>());
        if (map != null) {
            referenceType.get(type).addAll(toSetToscaReferences(map));
        }

    }

    private static <A extends DocToscaEntity<?>> void fillReferenceType(Map<String, Set<String>> referenceType,
            String type, List<Map<String, A>> list) {
        referenceType.putIfAbsent(type, new HashSet<>());
        if (list != null) {
            list.forEach(map -> referenceType.get(type).addAll(toSetToscaReferences(map)));
        }
    }

    private static <A extends DocToscaEntity<?>> Set<String> toSetToscaReferences(Map<String, A> map) {
        Set<String> result = new HashSet<>();
        for (var entity : map.values()) {
            result.add(entity.getDocConceptKey().getId()); // ref for type
            result.add(entity.getDocConceptKey().getName()); // ref for derived from
        }
        return result;
    }

    /**
     * Compare two maps of the same type, nulls are allowed.
     *
     * @param leftMap the first map
     * @param rightMap the second map
     * @return a measure of the comparison
     */
    public static <V extends Comparable<? super V>> int compareMaps(final Map<String, V> leftMap,
            final Map<String, V> rightMap) {
        if ((MapUtils.isEmpty(leftMap) && MapUtils.isEmpty(rightMap))) {
            return 0;
        }
        if (leftMap == null) {
            return 1;
        }

        if (rightMap == null) {
            return -1;
        }
        if (leftMap.size() != rightMap.size()) {
            return leftMap.hashCode() - rightMap.hashCode();
        }

        for (var leftEntry : leftMap.entrySet()) {
            var result = ObjectUtils.compare(leftEntry.getValue(), rightMap.get(leftEntry.getKey()));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    /**
     * Compare two lists of Map of the same type, nulls are allowed.
     *
     * @param leftCollection the first list
     * @param rightCollection the second list
     * @return a measure of the comparison
     */
    public static <V extends Comparable<? super V>> int compareCollections(final List<Map<String, V>> leftCollection,
            final List<Map<String, V>> rightCollection) {
        if ((CollectionUtils.isEmpty(leftCollection) && CollectionUtils.isEmpty(rightCollection))) {
            return 0;
        }
        if (leftCollection == null) {
            return 1;
        }

        if (rightCollection == null) {
            return -1;
        }
        if (leftCollection.size() != rightCollection.size()) {
            return leftCollection.hashCode() - rightCollection.hashCode();
        }

        for (var i = 0; i < leftCollection.size(); i++) {
            var result = compareMaps(leftCollection.get(i), rightCollection.get(i));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }
}
