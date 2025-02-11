/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.persistence.concepts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.Validated;

@Entity
@Table(name = "MessageJob")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaMessageJob extends Validated {

    @Id
    @NotNull
    private String jobId = UUID.randomUUID().toString();

    @Column(unique = true)
    @NotNull
    private String identificationId;

    @Column
    @NotNull
    private Timestamp jobStarted = TimestampHelper.nowTimestamp();

    public JpaMessageJob() {
        this(UUID.randomUUID().toString());
    }

    public JpaMessageJob(@NonNull final String identificationId) {
        this.identificationId = identificationId;
    }

    @Override
    public BeanValidationResult validate(@NonNull String fieldName) {
        var result = super.validate(fieldName);
        if (!result.isValid()) {
            return result;
        }

        return result;
    }
}
