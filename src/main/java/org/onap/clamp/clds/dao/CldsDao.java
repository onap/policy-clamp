/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.dao;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.onap.clamp.clds.model.CldsDbServiceCache;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.CldsModelInstance;
import org.onap.clamp.clds.model.CldsModelProp;
import org.onap.clamp.clds.model.CldsMonitoringDetails;
import org.onap.clamp.clds.model.CldsServiceData;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.model.ValueItem;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

/**
 * Data Access for CLDS Model tables.
 */
@Repository("cldsDao")
public class CldsDao {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsDao.class);
    private JdbcTemplate jdbcTemplateObject;
    private SimpleJdbcCall procGetModel;
    private SimpleJdbcCall procGetModelTemplate;
    private SimpleJdbcCall procSetModel;
    private SimpleJdbcCall procInsEvent;
    private SimpleJdbcCall procUpdEvent;
    private SimpleJdbcCall procSetTemplate;
    private SimpleJdbcCall procGetTemplate;
    private SimpleJdbcCall procDelAllModelInstances;
    private SimpleJdbcCall procInsModelInstance;
    private SimpleJdbcCall procDeleteModel;
    private static final String HEALTHCHECK = "Select 1";
    private static final String V_CONTROL_NAME_PREFIX = "v_control_name_prefix";
    private static final String V_CONTROL_NAME_UUID = "v_control_name_uuid";
   
    /**
     * Log message when instantiating
     */
    public CldsDao() {
        logger.info("CldsDao instantiating...");
    }

    /**
     * When dataSource is provided, instantiate spring jdbc objects.
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
        this.procDelAllModelInstances = new SimpleJdbcCall(dataSource).withProcedureName("del_all_model_instances");
        this.procDeleteModel = new SimpleJdbcCall(dataSource).withProcedureName("del_model");
    }

    /**
     * Get a model from the database given the model name.
     */
    public CldsModel getModel(String modelName) {
        return getModel(modelName, null);
    }

    /**
     * Get a model from the database given the controlNameUuid.
     */
    public CldsModel getModelByUuid(String controlNameUuid) {
        return getModel(null, controlNameUuid);
    }

    // Get a model from the database given the model name or a controlNameUuid.
    private CldsModel getModel(String modelName, String controlNameUuid) {
        CldsModel model = new CldsModel();
        model.setName(modelName);
        SqlParameterSource in = new MapSqlParameterSource().addValue("v_model_name", modelName)
                .addValue(V_CONTROL_NAME_UUID, controlNameUuid);
        Map<String, Object> out = logSqlExecution(procGetModel, in);
        populateModelProperties(model, out);
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
        populateModelProperties(model, out);
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
                .addValue(V_CONTROL_NAME_UUID, model.getControlNameUuid());
        Map<String, Object> out = logSqlExecution(procSetModel, in);
        model.setControlNamePrefix((String) out.get(V_CONTROL_NAME_PREFIX));
        model.setControlNameUuid((String) out.get(V_CONTROL_NAME_UUID));
        model.setId((String) (out.get("v_model_id")));
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
                        .addValue(V_CONTROL_NAME_UUID, model.getControlNameUuid())
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
                .addValue(V_CONTROL_NAME_PREFIX, controlNamePrefix).addValue(V_CONTROL_NAME_UUID, controlNameUuid)
                .addValue("v_user_id", cldsEvent.getUserid()).addValue("v_action_cd", cldsEvent.getActionCd())
                .addValue("v_action_state_cd", cldsEvent.getActionStateCd())
                .addValue("v_process_instance_id", cldsEvent.getProcessInstanceId());
        Map<String, Object> out = logSqlExecution(procInsEvent, in);
        event.setId((String) (out.get("v_event_id")));
        return event;
    }

    private String delAllModelInstances(String controlNameUUid) {
        SqlParameterSource in = new MapSqlParameterSource().addValue(V_CONTROL_NAME_UUID, controlNameUUid);
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
     * Return list of model names
     *
     * @return model names
     */
    public List<ValueItem> getBpmnNames() {
        String sql = "SELECT model_name FROM model ORDER BY 1;";
        return jdbcTemplateObject.query(sql, new ValueItemMapper());
    }

    /**
     * Update template in the database using parameter values and return updated
     * template object.
     *
     * @param template
     * @param userid
     */
    public void setTemplate(CldsTemplate template, String userid) {
        SqlParameterSource in = new MapSqlParameterSource().addValue("v_template_name", template.getName())
                .addValue("v_user_id", userid).addValue("v_template_bpmn_text", template.getBpmnText())
                .addValue("v_template_image_text", template.getImageText())
                .addValue("v_template_doc_text", template.getPropText());
        Map<String, Object> out = logSqlExecution(procSetTemplate, in);
        template.setId((String) (out.get("v_template_id")));
        template.setBpmnUserid((String) (out.get("v_template_bpmn_user_id")));
        template.setBpmnId((String) (out.get("v_template_bpmn_id")));
        template.setImageId((String) (out.get("v_template_image_id")));
        template.setImageUserid((String) out.get("v_template_image_user_id"));
        template.setPropId((String) (out.get("v_template_doc_id")));
        template.setPropUserid((String) out.get("v_template_doc_user_id"));
    }

    /**
     * Return list of template names
     *
     * @return template names
     */
    public List<ValueItem> getTemplateNames() {
        String sql = "SELECT template_name FROM template ORDER BY 1;";
        return jdbcTemplateObject.query(sql, new ValueItemMapper());
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

    public void clearServiceCache() {
        String clearCldsServiceCacheSql = "TRUNCATE clds_service_cache";
        jdbcTemplateObject.execute(clearCldsServiceCacheSql);
    }

    public CldsServiceData getCldsServiceCache(String invariantUUID) {
        CldsServiceData cldsServiceData = null;
        try {
            String getCldsServiceSQL = "SELECT * , TIMESTAMPDIFF(SECOND, timestamp, CURRENT_TIMESTAMP()) FROM clds_service_cache where invariant_service_id  = ? ";
            cldsServiceData = jdbcTemplateObject.queryForObject(getCldsServiceSQL, new Object[] { invariantUUID },
                    new CldsServiceDataMapper());
            if (cldsServiceData != null) {
                logger.info("CldsServiceData found in cache for Service Invariant ID:"
                        + cldsServiceData.getServiceInvariantUUID());
                return cldsServiceData;
            } else {
                logger.warn("CldsServiceData not found in cache for Service Invariant ID:" + invariantUUID);
                return null;
            }
        } catch (EmptyResultDataAccessException e) {
            logger.info("CldsServiceData not found in cache for Service Invariant ID: " + invariantUUID);
            logger.debug("CldsServiceData not found in cache for Service Invariant ID: " + invariantUUID, e);
            return null;
        }
    }

    public void setCldsServiceCache(CldsDbServiceCache cldsDBServiceCache) {
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

    public void doHealthCheck() {
        jdbcTemplateObject.execute(HEALTHCHECK);
    }

    /**
     * Method to get deployed/active models with model properties.
     * 
     * @return list of CldsModelProp
     */
    public List<CldsModelProp> getDeployedModelProperties() {
        List<CldsModelProp> cldsModelPropList = new ArrayList<>();
        String modelsSql = "select m.model_id, m.model_name, mp.model_prop_id, mp.model_prop_text FROM model m, model_properties mp, event e "
                + "WHERE m.model_prop_id = mp.model_prop_id and m.event_id = e.event_id and e.action_cd = 'DEPLOY'";
        List<Map<String, Object>> rows = jdbcTemplateObject.queryForList(modelsSql);
        CldsModelProp cldsModelProp = null;
        for (Map<String, Object> row : rows) {
            cldsModelProp = new CldsModelProp();
            cldsModelProp.setId((String) row.get("model_id"));
            cldsModelProp.setName((String) row.get("model_name"));
            cldsModelProp.setPropId((String) row.get("model_prop_id"));
            cldsModelProp.setPropText((String) row.get("model_prop_text"));
            cldsModelPropList.add(cldsModelProp);
        }
        return cldsModelPropList;
    }

    /**
     * Method to get deployed/active models with model properties.
     * 
     * @return list of CLDS-Monitoring-Details: CLOSELOOP_NAME | Close loop name
     *         used in the CLDS application (prefix: ClosedLoop- + unique
     *         ClosedLoop ID) MODEL_NAME | Model Name in CLDS application
     *         SERVICE_TYPE_ID | TypeId returned from the DCAE application when
     *         the ClosedLoop is submitted (DCAEServiceTypeRequest generated in
     *         DCAE application). DEPLOYMENT_ID | Id generated when the
     *         ClosedLoop is deployed in DCAE. TEMPLATE_NAME | Template used to
     *         generate the ClosedLoop model. ACTION_CD | Current state of the
     *         ClosedLoop in CLDS application.
     */
    public List<CldsMonitoringDetails> getCLDSMonitoringDetails() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        List<CldsMonitoringDetails> cldsMonitoringDetailsList = new ArrayList<CldsMonitoringDetails>();
        String modelsSql = "SELECT CONCAT(M.CONTROL_NAME_PREFIX, M.CONTROL_NAME_UUID) AS CLOSELOOP_NAME , M.MODEL_NAME, M.SERVICE_TYPE_ID, M.DEPLOYMENT_ID, T.TEMPLATE_NAME, E.ACTION_CD, E.USER_ID, E.TIMESTAMP "
                + "FROM MODEL M, TEMPLATE T, EVENT E "
                + "WHERE M.TEMPLATE_ID = T.TEMPLATE_ID AND M.EVENT_ID = E.EVENT_ID " + "ORDER BY ACTION_CD";
        List<Map<String, Object>> rows = jdbcTemplateObject.queryForList(modelsSql);
        CldsMonitoringDetails cldsMonitoringDetails = null;
        for (Map<String, Object> row : rows) {
            cldsMonitoringDetails = new CldsMonitoringDetails();
            cldsMonitoringDetails.setCloseloopName((String) row.get("CLOSELOOP_NAME"));
            cldsMonitoringDetails.setModelName((String) row.get("MODEL_NAME"));
            cldsMonitoringDetails.setServiceTypeId((String) row.get("SERVICE_TYPE_ID"));
            cldsMonitoringDetails.setDeploymentId((String) row.get("DEPLOYMENT_ID"));
            cldsMonitoringDetails.setTemplateName((String) row.get("TEMPLATE_NAME"));
            cldsMonitoringDetails.setAction((String) row.get("ACTION_CD"));
            cldsMonitoringDetails.setUserid((String) row.get("USER_ID"));
            cldsMonitoringDetails.setTimestamp(sdf.format(row.get("TIMESTAMP")));
            cldsMonitoringDetailsList.add(cldsMonitoringDetails);
        }
        return cldsMonitoringDetailsList;
    }

    /**
     * Method to delete model from database.
     * 
     * @param modelName
     */
    public void deleteModel(String modelName) {
        SqlParameterSource in = new MapSqlParameterSource().addValue("v_model_name", modelName);
        logSqlExecution(procDeleteModel, in);
    }
    
    private void populateModelProperties(CldsModel model, Map out) {
        // todo : rationalize
        model.setControlNamePrefix((String) out.get(V_CONTROL_NAME_PREFIX));
        model.setControlNameUuid((String) out.get(V_CONTROL_NAME_UUID));
        model.setId((String) (out.get("v_model_id")));
        model.setTemplateId((String) (out.get("v_template_id")));
        model.setTemplateName((String) (out.get("v_template_name")));
        model.setBpmnText((String) out.get("v_template_bpmn_text"));
        model.setPropText((String) out.get("v_model_prop_text"));
        model.setImageText((String) out.get("v_template_image_text"));
        model.setDocText((String) out.get("v_template_doc_text"));
        model.setBlueprintText((String) out.get("v_model_blueprint_text"));
        model.getEvent().setId((String) (out.get("v_event_id")));
        model.getEvent().setActionCd((String) out.get("v_action_cd"));
        model.getEvent().setActionStateCd((String) out.get("v_action_state_cd"));
        model.getEvent().setProcessInstanceId((String) out.get("v_event_process_instance_id"));
        model.getEvent().setUserid((String) out.get("v_event_user_id"));
        model.setTypeId((String) out.get("v_service_type_id"));
        model.setDeploymentId((String) out.get("v_deployment_id"));        	
    }    
}
