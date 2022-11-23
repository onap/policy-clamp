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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaEntity;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.models.base.Validated;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToscaServiceTemplateValidation {

    private static final String ROOT_KEY_NAME_SUFFIX = ".Root";

    /**
     * validate a serviceTemplate.
     *
     * @param result the result
     * @param serviceTemplate the serviceTemplate to validate
     */
    public static void validate(final BeanValidationResult result, DocToscaServiceTemplate serviceTemplate) {

        var references = DocUtil.getToscaReferences(serviceTemplate);

        validEntityTypeAncestors(serviceTemplate.getDataTypes(), references.get(DocUtil.REF_DATA_TYPES), result);
        validEntityTypeAncestors(serviceTemplate.getCapabilityTypes(), references.get(DocUtil.REF_CAPABILITY_TYPES),
                result);
        validEntityTypeAncestors(serviceTemplate.getNodeTypes(), references.get(DocUtil.REF_NODE_TYPES), result);
        validEntityTypeAncestors(serviceTemplate.getRelationshipTypes(), references.get(DocUtil.REF_RELATIONSHIP_TYPES),
                result);
        validEntityTypeAncestors(serviceTemplate.getPolicyTypes(), references.get(DocUtil.REF_POLICY_TYPES), result);

        if (serviceTemplate.getNodeTypes() != null) {
            for (var nodeType : serviceTemplate.getNodeTypes().values()) {
                validEntityTypeAncestors(nodeType.getRequirements(), references.get(DocUtil.REF_REQUIREMENTS), result);
            }
        }

        if (serviceTemplate.getToscaTopologyTemplate() != null) {
            validEntityTypeAncestors(serviceTemplate.getToscaTopologyTemplate().getNodeTemplates(),
                    references.get(DocUtil.REF_NODE_TEMPLATES), result);
            validEntityTypeAncestors(serviceTemplate.getToscaTopologyTemplate().getPolicies(),
                    references.get(DocUtil.REF_POLICIES), result);

            if (serviceTemplate.getToscaTopologyTemplate().getNodeTemplates() != null) {
                for (var nodeTemplate : serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().values()) {
                    validEntityTypeAncestors(nodeTemplate.getCapabilities(), references.get(DocUtil.REF_CAPABILITIES),
                            result);
                    validEntityTypeAncestors(nodeTemplate.getRequirements(), references.get(DocUtil.REF_REQUIREMENTS),
                            result);
                }
            }
        }

        validateReferencedDataTypes(result, serviceTemplate, references);

        validatePolicyTypesInPolicies(result, serviceTemplate, references);

    }

    /**
     * Validate that all data types referenced in policy types exist.
     *
     * @param result where the results are added
     */
    private static void validateReferencedDataTypes(final BeanValidationResult result,
            DocToscaServiceTemplate serviceTemplate, Map<String, Set<String>> references) {
        if (serviceTemplate.getDataTypes() != null) {
            for (var dataType : serviceTemplate.getDataTypes().values()) {
                validateReferencedDataTypesExists(result, dataType.getReferencedDataTypes(), references);
            }
        }

        if (serviceTemplate.getPolicyTypes() != null) {
            for (var policyType : serviceTemplate.getPolicyTypes().values()) {
                validateReferencedDataTypesExists(result, policyType.getReferencedDataTypes(), references);
            }
        }
        if (serviceTemplate.getNodeTypes() != null) {
            for (var nodeType : serviceTemplate.getNodeTypes().values()) {
                validateReferencedDataTypesExists(result, nodeType.getReferencedDataTypes(), references);
            }
        }
    }

    /**
     * Validate that the referenced data types exist for a collection of data type keys.
     *
     * @param dataTypeKeyCollection the data type key collection
     * @param result where the results are added
     */
    private static void validateReferencedDataTypesExists(final BeanValidationResult result,
            final Collection<DocConceptKey> dataTypeKeyCollection, Map<String, Set<String>> references) {
        for (DocConceptKey dataTypeKey : dataTypeKeyCollection) {
            if (!isTypePresent(dataTypeKey, references.get(DocUtil.REF_DATA_TYPES))) {
                result.addResult("data type", dataTypeKey.getId(), ValidationStatus.INVALID, Validated.NOT_FOUND);
            }
        }
    }

    /**
     * Validate that all policy types referenced in policies exist.
     *
     * @param result where the results are added
     */
    private static void validatePolicyTypesInPolicies(final BeanValidationResult result,
            DocToscaServiceTemplate serviceTemplate, Map<String, Set<String>> references) {
        if (serviceTemplate.getToscaTopologyTemplate() == null) {
            return;
        }

        if (serviceTemplate.getToscaTopologyTemplate().getPolicies() != null) {
            for (var policy : serviceTemplate.getToscaTopologyTemplate().getPolicies().values()) {
                var key = policy.getTypeDocConceptKey();
                if (!isTypePresent(key, references.get(DocUtil.REF_POLICY_TYPES))) {
                    result.addResult("policy type", key, ValidationStatus.INVALID, Validated.NOT_FOUND);
                }
            }
        }
        if (serviceTemplate.getToscaTopologyTemplate().getNodeTemplates() != null) {
            for (var nodeTemplate : serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().values()) {
                var key = nodeTemplate.getTypeDocConceptKey();
                if (!isTypePresent(key, references.get(DocUtil.REF_NODE_TYPES))) {
                    result.addResult("node Template", key, ValidationStatus.INVALID, Validated.NOT_FOUND);
                }
            }
        }
    }

    private static boolean isTypePresent(String key, Set<String> reference) {
        if (reference == null || reference.isEmpty()) {
            return false;
        }
        return reference.contains(key);
    }

    private static boolean isTypePresent(DocConceptKey key, Set<String> reference) {
        if (reference == null || reference.isEmpty()) {
            return false;
        }
        return reference.contains(key.getId());
    }

    private static String extractDerivedFrom(DocToscaEntity<?> entityType, final BeanValidationResult result) {
        if (entityType.getDerivedFrom() == null) {
            return null;
        }
        var parentEntityTypeKey = entityType.getDerivedFrom();

        if (parentEntityTypeKey.endsWith(ROOT_KEY_NAME_SUFFIX)) {
            return null;
        }
        if (entityType.getName().equals(parentEntityTypeKey)) {
            result.addResult("entity type", entityType.getDocConceptKey().getId(), ValidationStatus.INVALID,
                    "ancestor of itself");
            return null;
        }
        return parentEntityTypeKey;
    }

    /**
     * validate all the ancestors of an entity type.
     *
     * @param entityTypes the set of entity types that exist
     * @param result the result of the ancestor search with any warnings or errors
     */
    private static void validEntityTypeAncestors(Map<String, ? extends DocToscaEntity<?>> entityTypes,
            Set<String> reference, @NonNull final BeanValidationResult result) {
        if (entityTypes != null) {
            for (var entityType : entityTypes.values()) {
                var parentEntityTypeKey = extractDerivedFrom(entityType, result);
                if (parentEntityTypeKey == null) {
                    continue;
                }
                if (!isTypePresent(parentEntityTypeKey, reference)) {
                    result.addResult("parent", parentEntityTypeKey, ValidationStatus.INVALID,
                            Validated.NOT_FOUND);
                }
            }
        }
    }

    /**
     * validate all the ancestors of an entity type.
     *
     * @param entityTypesList the set of entity types that exist
     * @param result the result of the ancestor search with any warnings or errors
     */
    private static <T extends DocToscaEntity<?>> void validEntityTypeAncestors(List<Map<String, T>> entityTypesList,
            Set<String> reference, @NonNull final BeanValidationResult result) {
        if (entityTypesList != null) {
            for (var entityTypes : entityTypesList) {
                for (var entityType : entityTypes.values()) {
                    var parentEntityTypeKey = extractDerivedFrom(entityType, result);
                    if (parentEntityTypeKey == null) {
                        continue;
                    }
                    if (!isTypePresent(parentEntityTypeKey, reference)) {
                        result.addResult("parent", parentEntityTypeKey, ValidationStatus.INVALID,
                                Validated.NOT_FOUND);
                    }
                }
            }
        }
    }

}
