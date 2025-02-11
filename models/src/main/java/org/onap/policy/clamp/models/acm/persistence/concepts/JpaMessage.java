/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.Validated;

@Entity
@Table(name = "Message", indexes = {@Index(name = "m_identificationId", columnList = "identificationId")})
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
public class JpaMessage extends Validated implements PfAuthorative<DocMessage> {

    @Id
    @NotNull
    private String messageId = UUID.randomUUID().toString();

    @Column
    @NotNull
    // instanceId or compositionId
    private String identificationId;

    @Column
    @NotNull
    private Timestamp lastMsg = TimestampHelper.nowTimestamp();

    @Lob
    @Column(length = 100000)
    @Convert(converter = StringToDocMessage.class)
    @NotNull
    private DocMessage docMessage;

    public JpaMessage() {
        this(UUID.randomUUID().toString(), new DocMessage());
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaMessage(@NonNull final JpaMessage copyConcept) {
        this.messageId = copyConcept.messageId;
        this.identificationId = copyConcept.identificationId;
        this.lastMsg = copyConcept.lastMsg;
        fromAuthorative(copyConcept.docMessage);
    }

    public JpaMessage(@NonNull final String identificationId, @NonNull final DocMessage docMessage) {
        this.identificationId = identificationId;
        fromAuthorative(docMessage);
    }

    @Override
    public BeanValidationResult validate(@NonNull String fieldName) {
        var result = super.validate(fieldName);
        if (!result.isValid()) {
            return result;
        }

        return result;
    }

    @Override
    public DocMessage toAuthorative() {
        return new DocMessage(this.docMessage);
    }

    @Override
    public void fromAuthorative(@NonNull final DocMessage docMessage) {
        this.docMessage = new DocMessage(docMessage);
        this.docMessage.setMessageId(messageId);
    }
}
