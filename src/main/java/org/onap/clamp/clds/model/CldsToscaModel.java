/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.model;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.tosca.ToscaYamlToJsonConvertor;

public class CldsToscaModel extends CldsToscaModelRevision {

    private String id;
    private String policyType;
    private String toscaModelName;

    /**
     * Construct
     */
    public CldsToscaModel () {
    }

    /**
     * Creates or updates Tosca Model to DB
     *
     * @param cldsDao
     * @param userId
     */
    public CldsToscaModel save(CldsDao cldsDao, ClampProperties refProp, PolicyClient policyClient, String userId) {
        CldsToscaModel cldsToscaModel = null;
        refProp.getStringList("tosca.policyTypes", ",").stream().forEach(policyType -> {
            if (StringUtils.containsIgnoreCase(this.getToscaModelName(), policyType)) {
                this.setPolicyType(policyType);
            }
        });

        ToscaYamlToJsonConvertor convertor = new ToscaYamlToJsonConvertor(cldsDao);
        this.setToscaModelJson(convertor.parseToscaYaml(this.getToscaModelYaml()));
        List<CldsToscaModel> toscaModels = cldsDao.getToscaModelByName(this.getToscaModelName());
        if (toscaModels != null && !toscaModels.isEmpty()) {
            CldsToscaModel toscaModel = toscaModels.stream().findFirst().get();
            this.setVersion(incrementVersion(toscaModel.getVersion()));
            this.setId(toscaModel.getId());
            this.setUserId(userId);
            if (refProp.getStringValue("import.tosca.model").equalsIgnoreCase("true")) {
                policyClient.importToscaModel(this);
            }
            cldsToscaModel = cldsDao.updateToscaModelWithNewVersion(this, userId);
        } else {
            this.setVersion(1);
            if (refProp.getStringValue("import.tosca.model").equalsIgnoreCase("true")) {
                policyClient.importToscaModel(this);
            }
            cldsToscaModel = cldsDao.insToscaModel(this, userId);
        }
        return cldsToscaModel;
    }

    private double incrementVersion(double curVersion) {
        return curVersion + 1;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *        the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the policyType
     */
    public String getPolicyType() {
        return policyType;
    }

    /**
     * @param policyType
     *        the policyType to set
     */
    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    /**
     * @return the toscaModelName
     */
    public String getToscaModelName() {
        return toscaModelName;
    }

    /**
     * @param toscaModelName
     *        the toscaModelName to set
     */
    public void setToscaModelName(String toscaModelName) {
        this.toscaModelName = toscaModelName;
    }

}
