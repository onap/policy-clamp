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

import org.onap.clamp.clds.dao.CldsDao;

public class CldsToscaModel extends CldsToscaModelRevision {

    private String id;
    private String policyType;
    private String toscaModelName;
    private String toscaModelYaml;

    /**
     * Creates or updates Tosca Model to DB
     *
     * @param cldsDao
     * @param userId
     */
    public CldsToscaModel save(CldsDao cldsDao, String userId) {
        CldsToscaModel cldsToscaModel = null;
        // TODO tosca parsing logic
        this.setToscaModelJson("{}");
        this.setPolicyType("Aging");// TODO update with subString or node_type from the model name
        List<CldsToscaModel> toscaModels = cldsDao.getToscaModelByName(this.getToscaModelName());
        if (toscaModels != null && !toscaModels.isEmpty()) {
            CldsToscaModel toscaModel = toscaModels.stream().findFirst().get();
            // CldsToscaModelRevision modelRevision =
            // revisions.stream().max(Comparator.comparingDouble(CldsToscaModelRevision::getVersion)).get();
            this.setVersion(incrementVersion(toscaModel.getVersion()));
            this.setId(toscaModel.getId());
            this.setUserId(userId);
            cldsToscaModel = cldsDao.updateToscaModelWithNewVersion(this, userId);
        } else {
            this.setVersion(1);
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

    /**
     * @return the toscaModelYaml
     */
    @Override
    public String getToscaModelYaml() {
        return toscaModelYaml;
    }

    /**
     * @param toscaModelYaml
     *        the toscaModelYaml to set
     */
    @Override
    public void setToscaModelYaml(String toscaModelYaml) {
        this.toscaModelYaml = toscaModelYaml;
    }

}
