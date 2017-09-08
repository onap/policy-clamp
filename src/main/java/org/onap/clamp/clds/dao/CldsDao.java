/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.dao;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.onap.clamp.clds.model.CldsDBServiceCache;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.CldsModelInstance;
import org.onap.clamp.clds.model.CldsServiceData;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.model.ValueItem;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

/**
 * Data Access for CLDS Model tables.
 */
@Repository("cldsDao")
public class CldsDao {

    protected static final EELFLogger logger        = EELFManager.getInstance().getLogger(CldsDao.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    private JdbcTemplate              jdbcTemplateObject;
    private SimpleJdbcCall            procGetModel;
    private SimpleJdbcCall            procGetModelTemplate;
    private SimpleJdbcCall            procSetModel;
    private SimpleJdbcCall            procInsEvent;
    private SimpleJdbcCall            procUpdEvent;
    private SimpleJdbcCall            procSetTemplate;
    private SimpleJdbcCall            procGetTemplate;
    private SimpleJdbcCall            procDelAllModelInstances;
    private SimpleJdbcCall            procInsModelInstance;
    private SimpleJdbcCall            procDelModelInstance;

    private static final String       HEALTHCHECK   = "Select 1";

    /**
     * Log message when instantiating
     */
    public CldsDao() {
        logger.info("CldsDao instantiating...");
    }

    /**
     * When dataSource is provided, instantiate spring jdbc objects.
     *
     * @param dataSource
     */
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplateObject = new JdbcTemplate(dataSource);
        this.procGetModel = new SimpleJdbcCall(dataSource).withProcedureName("get_model");
        this.procGetModelTemplate = new SimpleJdbcCall(dataSource).withProcedureName("get_model_template");
        this.procSetModel = new SimpleJdbcCall(dataSource).withProcedureName("set_model");
        this.procInsEvent = new SimpleJdbcCall(dataSource).withProcedureName("ins_event");
        this.procUpdEvent = new SimpleJdbcCall(dataSource).withProcedureName("upd_event");
        this.procGetTemplate = new SimpleJdbcCall(dataSource).withProcedureName("get_template");
        this.procSetTemplate = new SimpleJdbcCall(dataSource).withProcedureName("set_template");
        this.procInsModelInstance = new SimpleJdbcCall(dataSource).withProcedureName("ins_model_instance");
        this.procDelModelInstance = new SimpleJdbcCall(dataSource).withProcedureName("del_model_instance");
        this.procDelAllModelInstances = new SimpleJdbcCall(dataSource).withProcedureName("del_all_model_instances");
    }

    /**
     * Get a model from the database given the model name.
     *
     * @param modelName
     * @return model
     */
    public CldsModel getModel(String modelName) {
        return getModel(modelName, null);
    }

    /**
     * Get a model from the database given the controlNameUuid.
     *
     * @param controlNameUuid
     * @return model
     */
    public CldsModel getModelByUuid(String controlNameUuid) {
        return getModel(null, controlNameUuid);
    }

    /**
     * Get a model from the database given the model name or a controlNameUuid.
     *
     * @param modelName
     * @return model
     */
    private CldsModel getModel(String modelName, String controlNameUuid) {
        CldsModel model = new CldsModel();
        model.setName(modelName);
        SqlParameterSource in = new MapSqlParameterSource().addValue("v_model_name", modelName)
                .addValue("v_control_name_uuid", controlNameUuid);
        Map<String, Object> out = logSqlExecution(procGetModel, in);
        model.setControlNamePrefix((String) out.get("v_control_name_prefix"));
        model.setControlNameUuid((String) out.get("v_control_name_uuid"));
        model.setId((String) (out.get("v_model_id")));
        model.setTemplateId((String) (out.get("v_template_id")));
        model.setTemplateName((String) (out.get("v_template_name")));
        model.setBpmnId((String) (out.get("v_template_bpmn_id")));
        model.setBpmnUserid((String) out.get("v_template_bpmn_user_id"));
        model.setBpmnText((String) out.get("v_template_bpmn_text"));
        model.setPropId((String) (out.get("v_model_prop_id")));
        model.setPropUserid((String) out.get("v_model_prop_user_id"));
        model.setPropText((String) out.get("v_model_prop_text"));
        model.setImageId((String) (out.get("v_template_image_id")));
        model.setImageUserid((String) out.get("v_template_image_user_id"));
        model.setImageText((String) out.get("v_template_image_text"));
        model.setDocId((String) (out.get("v_template_doc_id")));
        model.setDocUserid((String) out.get("v_template_doc_user_id"));
        model.setDocText((String) out.get("v_template_doc_text"));
        model.setBlueprintText((String) out.get("v_model_blueprint_text"));
        model.getEvent().setId((String) (out.get("v_event_id")));
        model.getEvent().setActionCd((String) out.get("v_action_cd"));
        model.getEvent().setActionStateCd((String) out.get("v_action_state_cd"));
        model.getEvent().setProcessInstanceId((String) out.get("v_event_process_instance_id"));
        model.getEvent().setUserid((String) out.get("v_event_user_id"));
        model.setTypeId((String) out.get("v_service_type_id"));
        model.setDeploymentId((String) out.get("v_deployment_id"));
        return model;
    }

    /**
     * Get a model and template information from the database given the model
     * name.
     *
     * @param modelName
     * @return model
     */
    public CldsModel getModelTemplate(String modelName) {
        CldsModel model = new CldsModel();
        model.setName(modelName);
        SqlParameterSource in = new MapSqlParameterSource().addValue("v_model_name", modelName);
        Map<String, Object> out = logSqlExecution(procGetModelTemplate, in);
        // todo : rationalize
        model.setControlNamePrefix((String) out.get("v_control_name_prefix"));
        model.setControlNameUuid((String) out.get("v_control_name_uuid"));
        model.setId((String) (out.get("v_model_id")));
        model.setTemplateId((String) (out.get("v_template_id")));
        model.setTemplateName((String) (out.get("v_template_name")));
        model.setBpmnId((String) (out.get("v_template_bpmn_id")));
        model.setBpmnUserid((String) out.get("v_template_bpmn_user_id"));
        model.setBpmnText((String) out.get("v_template_bpmn_text"));
        model.setPropId((String) (out.get("v_model_prop_id")));
        model.setPropUserid((String) out.get("v_model_prop_user_id"));
        model.setPropText((String) out.get("v_model_prop_text"));
        model.setImageId((String) (out.get("v_template_image_id")));
        model.setImageUserid((String) out.get("v_template_image_user_id"));
        model.setImageText((String) out.get("v_template_image_text"));
        model.setDocId((String) (out.get("v_template_doc_id")));
        model.setDocUserid((String) out.get("v_template_doc_user_id"));
        model.setDocText((String) out.get("v_template_doc_text"));
        model.setBlueprintText((String) out.get("v_model_blueprint_text"));
        model.getEvent().setId((String) (out.get("v_event_id")));
        model.getEvent().setActionCd((String) out.get("v_action_cd"));
        model.getEvent().setActionStateCd((String) out.get("v_action_state_cd"));
        model.getEvent().setProcessInstanceId((String) out.get("v_event_process_instance_id"));
        model.getEvent().setUserid((String) out.get("v_event_user_id"));
        model.setTypeId((String) out.get("v_service_type_id"));
        model.setDeploymentId((String) out.get("v_deployment_id"));

        Map<String, Object> modelResults = logSqlExecution(procGetModel, in);
        Object modelResultObject = modelResults.get("#result-set-1");
        if (modelResultObject != null && modelResultObject instanceof ArrayList) {
            List<Object> modelInstanceRs = (List<Object>) modelResultObject;
            for (Object currModelInstance : modelInstanceRs) {
                if (currModelInstance != null && currModelInstance instanceof HashMap) {
                    HashMap<String, String> modelInstanceMap = (HashMap<String, String>) currModelInstance;
                    CldsModelInstance modelInstance = new CldsModelInstance();
                    modelInstance.setModelInstanceId(modelInstanceMap.get("model_instance_id"));
                    modelInstance.setVmName(modelInstanceMap.get("vm_name"));
                    modelInstance.setLocation(modelInstanceMap.get("location"));
                    model.getCldsModelInstanceList().add(modelInstance);
                    logger.info("value of currModel: {}", currModelInstance);
                }
            }
        }
        return model;
    }

    /**
     * Update model in the database using parameter values and return updated
     * model object.
     *
     * @param model
     * @param userid
     * @return
     */
    public CldsModel setModel(CldsModel model, String userid) {
        SqlParameterSource in = new MapSqlParameterSource().addValue("v_model_name", model.getName())
                .addValue("v_template_id", model.getTemplateId()).addValue("v_user_id", userid)
                .addValue("v_model_prop_text", model.getPropText())
                .addValue("v_model_blueprint_text", model.getBlueprintText())
                .addValue("v_service_type_id", model.getTypeId()).addValue("v_deployment_id", model.getDeploymentId())
                .addValue("v_control_name_prefix", model.getControlNamePrefix())
                .addValue("v_control_name_uuid", model.getControlNameUuid());
        Map<String, Object> out = logSqlExecution(procSetModel, in);
        model.setControlNamePrefix((String) out.get("v_control_name_prefix"));
        model.setControlNameUuid((String) out.get("v_control_name_uuid"));
        model.setId((String) (out.get("v_model_id")));
        model.setPropId((String) (out.get("v_model_prop_id")));
        model.setPropUserid((String) (out.get("v_model_prop_user_id")));
        model.setBlueprintId((String) (out.get("v_model_blueprint_id")));
        model.setBlueprintUserid((String) out.get("v_model_blueprint_user_id"));
        model.getEvent().setId((String) (out.get("v_event_id")));
        model.getEvent().setActionCd((String) out.get("v_action_cd"));
        model.getEvent().setActionStateCd((String) out.get("v_action_state_cd"));
        model.getEvent().setProcessInstanceId((String) out.get("v_event_process_instance_id"));
        model.getEvent().setUserid((String) out.get("v_event_user_id"));
        return model;
    }

    /**
     * Inserts new modelInstance in the database using parameter values and
     * return updated model object.
     *
     * @param model
     * @param modelInstancesList
     * @return
     */
    public void insModelInstance(CldsModel model, List<CldsModelInstance> modelInstancesList) {
        // Delete all existing model instances for given controlNameUUID
        logger.debug("deleting instances for: {}", model.getControlNameUuid());
        delAllModelInstances(model.getControlNameUuid());

        if (modelInstancesList == null) {
            logger.debug("modelInstancesList == null");
        } else {
            for (CldsModelInstance currModelInstance : modelInstancesList) {
                logger.debug("v_control_name_uuid={}", model.getControlNameUuid());
                logger.debug("v_vm_name={}", currModelInstance.getVmName());
                logger.debug("v_location={}", currModelInstance.getLocation());
                SqlParameterSource in = new MapSqlParameterSource()
                        .addValue("v_control_name_uuid", model.getControlNameUuid())
                        .addValue("v_vm_name", currModelInstance.getVmName())
                        .addValue("v_location", currModelInstance.getLocation());
                Map<String, Object> out = logSqlExecution(procInsModelInstance, in);
                model.setId((String) (out.get("v_model_id")));
                CldsModelInstance modelInstance = new CldsModelInstance();
                modelInstance.setLocation(currModelInstance.getLocation());
                modelInstance.setVmName(currModelInstance.getVmName());
                modelInstance.setModelInstanceId((String) (out.get("v_model_instance_id")));
                model.getCldsModelInstanceList().add(modelInstance);
            }
        }
    }

    /**
     * Delete a list of modelInstance from the database using parameter values
     * and returns updated model object. This method is defunct - DCAE Proxy
     * will not undeploy individual instances. It will send an empty list of
     * deployed instances to indicate all have been removed. Or it will send an
     * updated list to indicate those that are still deployed with any not on
     * the list considered undeployed.
     *
     * @param controlNameUUid
     * @param modelInstancesList
     * @return
     */
    private CldsModel delModelInstance(String controlNameUUid, List<CldsModelInstance> modelInstancesList) {
        CldsModel model = new CldsModel();
        for (CldsModelInstance currModelInstance : modelInstancesList) {
            SqlParameterSource in = new MapSqlParameterSource().addValue("v_control_name_uuid", controlNameUUid)
                    .addValue("v_vm_name", currModelInstance.getVmName());
            Map<String, Object> out = logSqlExecution(procDelModelInstance, in);
            model.setId((String) (out.get("v_model_id")));
        }
        return model;
    }

    /**
     * Insert an event in the database - require either modelName or
     * controlNamePrefix/controlNameUuid.
     *
     * @param modelName
     * @param controlNamePrefix
     * @param controlNameUuid
     * @param cldsEvent
     * @return
     */
    public CldsEvent insEvent(String modelName, String controlNamePrefix, String controlNameUuid, CldsEvent cldsEvent) {
        CldsEvent event = new CldsEvent();
        SqlParameterSource in = new MapSqlParameterSource().addValue("v_model_name", modelName)
                .addValue("v_control_name_prefix", controlNamePrefix).addValue("v_control_name_uuid", controlNameUuid)
                .addValue("v_user_id", cldsEvent.getUserid()).addValue("v_action_cd", cldsEvent.getActionCd())
                .addValue("v_action_state_cd", cldsEvent.getActionStateCd())
                .addValue("v_process_instance_id", cldsEvent.getProcessInstanceId());
        Map<String, Object> out = logSqlExecution(procInsEvent, in);
        event.setId((String) (out.get("v_event_id")));
        return event;
    }

    /**
     * Method to delete all model instances based on controlNameUUID
     *
     * @param controlNameUUid
     * @return
     */
    private String delAllModelInstances(String controlNameUUid) {
        SqlParameterSource in = new MapSqlParameterSource().addValue("v_control_name_uuid", controlNameUUid);
        Map<String, Object> out = logSqlExecution(procDelAllModelInstances, in);
        return (String) (out.get("v_model_id"));
    }

    /**
     * Update event with process instance id.
     *
     * @param eventId
     * @param processInstanceId
     */
    public void updEvent(String eventId, String processInstanceId) {
        SqlParameterSource in = new MapSqlParameterSource().addValue("v_event_id", eventId)
                .addValue("v_process_instance_id", processInstanceId);
        logSqlExecution(procUpdEvent, in);
    }

    /**
     * Generic mapper for list of values
     */
    private static final class ValueItemMapper implements RowMapper<ValueItem> {
        @Override
        public ValueItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            ValueItem item = new ValueItem();
            item.setValue(rs.getString(1));
            return item;
        }
    }

    /**
     * Generic mapper for CldsDBServiceCache
     */
    private static final class CldsServiceDataMapper implements RowMapper<CldsServiceData> {
        @Override
        public CldsServiceData mapRow(ResultSet rs, int rowNum) throws SQLException {
            CldsServiceData cldsServiceData = new CldsServiceData();
            long age;
            age = rs.getLong(5);
            Blob blob = rs.getBlob(4);
            InputStream is = blob.getBinaryStream();
            ObjectInputStream oip;
            try {
                oip = new ObjectInputStream(is);
                cldsServiceData = (CldsServiceData) oip.readObject();
                cldsServiceData.setAgeOfRecord(age);
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Error caught while retrieving cldsServiceData from database", e);
            }
            return cldsServiceData;
        }
    }

    /**
     * Return list of model names
     *
     * @return model names
     */
    public List<ValueItem> getBpmnNames() {
        String SQL = "SELECT model_name FROM model ORDER BY 1;";
        return jdbcTemplateObject.query(SQL, new ValueItemMapper());
    }

    /**
     * Update template in the database using parameter values and return updated
     * template object.
     *
     * @param template
     * @param userid
     * @return
     */
    public CldsTemplate setTemplate(CldsTemplate template, String userid) {
        SqlParameterSource in = new MapSqlParameterSource().addValue("v_template_name", template.getName())
                .addValue("v_user_id", userid).addValue("v_template_bpmn_text", template.getBpmnText())
                .addValue("v_template_image_text", template.getImageText())
                .addValue("v_template_doc_text", template.getPropText());
        Map<String, Object> out = logSqlExecution(procSetTemplate, in);
        template.setId((String) (out.get("v_template_id")));
        template.setBpmnUserid((String) (out.get("v_template_bpmn_user_id")));
        template.setBpmnId((String) (out.get("v_template_bpmn_id")));
        template.setBpmnText((String) (out.get("v_template_bpmn_text")));
        template.setImageId((String) (out.get("v_template_image_id")));
        template.setImageUserid((String) out.get("v_template_image_user_id"));
        template.setImageText((String) out.get("v_template_image_text"));
        template.setPropId((String) (out.get("v_template_doc_id")));
        template.setPropUserid((String) out.get("v_template_doc_user_id"));
        template.setPropText((String) out.get("v_template_doc_text"));
        return template;
    }

    /**
     * Return list of template names
     *
     * @return template names
     */
    public List<ValueItem> getTemplateNames() {
        String SQL = "SELECT template_name FROM template ORDER BY 1;";
        return jdbcTemplateObject.query(SQL, new ValueItemMapper());
    }

    /**
     * Get a template from the database given the model name.
     *
     * @param templateName
     * @return model
     */
    public CldsTemplate getTemplate(String templateName) {
        CldsTemplate template = new CldsTemplate();
        template.setName(templateName);
        SqlParameterSource in = new MapSqlParameterSource().addValue("v_template_name", templateName);
        Map<String, Object> out = logSqlExecution(procGetTemplate, in);
        template.setId((String) (out.get("v_template_id")));
        template.setBpmnUserid((String) (out.get("v_template_bpmn_user_id")));
        template.setBpmnId((String) (out.get("v_template_bpmn_id")));
        template.setBpmnText((String) (out.get("v_template_bpmn_text")));
        template.setImageId((String) (out.get("v_template_image_id")));
        template.setImageUserid((String) out.get("v_template_image_user_id"));
        template.setImageText((String) out.get("v_template_image_text"));
        template.setPropId((String) (out.get("v_template_doc_id")));
        template.setPropUserid((String) out.get("v_template_doc_user_id"));
        template.setPropText((String) out.get("v_template_doc_text"));
        return template;
    }

    public CldsServiceData getCldsServiceCache(String invariantUUID) {
        CldsServiceData cldsServiceData = null;
        List<CldsServiceData> cldsServiceDataList = new ArrayList<>();
        try {
            String getCldsServiceSQL = "SELECT * , TIMESTAMPDIFF(SECOND, timestamp, CURRENT_TIMESTAMP()) FROM clds_service_cache where invariant_service_id  = ? ";
            cldsServiceData = jdbcTemplateObject.queryForObject(getCldsServiceSQL, new Object[] { invariantUUID },
                    new CldsServiceDataMapper());
            logger.info("value of cldsServiceDataList: {}", cldsServiceDataList);
        } catch (EmptyResultDataAccessException e) {
            logger.info("cache row not found for invariantUUID: {}", invariantUUID);
        }
        return cldsServiceData;
    }

    public void setCldsServiceCache(CldsDBServiceCache cldsDBServiceCache) {
        if (cldsDBServiceCache != null && cldsDBServiceCache.getInvariantId() != null
                && cldsDBServiceCache.getServiceId() != null) {
            String invariantUuid = cldsDBServiceCache.getInvariantId();
            String serviceUuid = cldsDBServiceCache.getServiceId();
            InputStream is = cldsDBServiceCache.getCldsDataInstream();
            String insertCldsServiceCacheSql = "INSERT INTO clds_service_cache"
                    + "(invariant_service_id,service_id,timestamp,object_data) VALUES"
                    + "(?,?,CURRENT_TIMESTAMP,?) ON DUPLICATE KEY UPDATE invariant_service_id = VALUES(invariant_service_id) , timestamp = CURRENT_TIMESTAMP , object_data = VALUES(object_data) ";
            jdbcTemplateObject.update(insertCldsServiceCacheSql, invariantUuid, serviceUuid, is);
        }
    }

    private static Map<String, Object> logSqlExecution(SimpleJdbcCall call, SqlParameterSource source) {
        try {
            return call.execute(source);
        } catch (Exception e) {
            logger.error("Exception occured in " + source.getClass().getCanonicalName() + ": " + e);
            throw e;
        }
    }

    public void doHealthCheck() throws SQLException, IOException {
        jdbcTemplateObject.execute(HEALTHCHECK);
    }

}
