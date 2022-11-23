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

package org.onap.policy.clamp.models.acm.document.concepts;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConstraint;

@Data
@NoArgsConstructor
public class DocToscaConstraint implements PfAuthorative<ToscaConstraint>, Serializable {

    private static final long serialVersionUID = 1L;

    @SerializedName("valid_values")
    private List<String> validValues;

    private String equal;

    @SerializedName("greater_than")
    private String greaterThan;

    @SerializedName("greater_or_equal")
    private String greaterOrEqual;

    @SerializedName("less_than")
    private String lessThan;

    @SerializedName("less_or_equal")
    private String lessOrEqual;

    @SerializedName("in_range")
    private List<String> rangeValues;

    public DocToscaConstraint(ToscaConstraint toscaConstraint) {
        fromAuthorative(toscaConstraint);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaConstraint(final DocToscaConstraint copyConcept) {
        this.validValues = copyConcept.validValues == null ? null : new ArrayList<>(copyConcept.validValues);
        this.equal = copyConcept.equal;
        this.greaterThan = copyConcept.greaterThan;
        this.greaterOrEqual = copyConcept.greaterOrEqual;
        this.lessThan = copyConcept.lessThan;
        this.lessOrEqual = copyConcept.lessOrEqual;
        this.rangeValues = copyConcept.rangeValues == null ? null : new ArrayList<>(copyConcept.rangeValues);
    }

    @Override
    public ToscaConstraint toAuthorative() {
        var toscaConstraint = new ToscaConstraint();
        if (validValues != null) {
            toscaConstraint.setValidValues(new ArrayList<>(validValues));
        }
        toscaConstraint.setEqual(equal);
        toscaConstraint.setGreaterThan(greaterThan);
        toscaConstraint.setGreaterOrEqual(greaterOrEqual);
        toscaConstraint.setLessOrEqual(lessOrEqual);
        if (rangeValues != null) {
            toscaConstraint.setRangeValues(new ArrayList<>(rangeValues));
        }
        return toscaConstraint;
    }

    @Override
    public void fromAuthorative(ToscaConstraint toscaConstraint) {
        if (toscaConstraint.getValidValues() != null) {
            validValues = new ArrayList<>(toscaConstraint.getValidValues());
        }
        equal = toscaConstraint.getEqual();
        greaterThan = toscaConstraint.getGreaterThan();
        greaterOrEqual = toscaConstraint.getGreaterOrEqual();
        lessThan = toscaConstraint.getLessThan();
        lessOrEqual = toscaConstraint.getLessOrEqual();
        if (toscaConstraint.getRangeValues() != null) {
            rangeValues = new ArrayList<>(toscaConstraint.getRangeValues());
        }
    }
}
