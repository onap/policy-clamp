package org.onap.clamp.clds.client.req;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import org.onap.clamp.clds.model.prop.Global;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.prop.Tca;
import org.onap.clamp.clds.model.prop.TcaItem;
import org.onap.clamp.clds.model.prop.TcaThreshhold;
import org.onap.clamp.clds.model.refprop.RefProp;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Construct a Policy for Tca/MTca Service request given CLDS objects. 
 * 
 *
 */
public class TcaMPolicyReq {
        private static final Logger logger = Logger.getLogger(StringMatchPolicyReq.class.getName());
        
        /**
         * Format Tca Policy request
         * 
         * @param refProp
         * @param prop
         * @return
         * @throws JsonParseException
         * @throws JsonMappingException
         * @throws IOException
         */
        public static String formatTca(RefProp refProp, ModelProperties prop) throws JsonParseException, JsonMappingException, IOException {
                Global global = prop.getGlobal();
                String service = global.getService();
                
                Tca tca = prop.getTca();
                prop.setCurrentModelElementId(tca.getId());
                ObjectNode rootNode = (ObjectNode)refProp.getJsonTemplate("tca.template", service);
                rootNode.put("policyName", prop.getCurrentPolicyScopeAndPolicyName());
                ObjectNode content = rootNode.with("content");
                appendSignatures(refProp, service, content, tca, prop);
                
                String tcaPolicyReq = rootNode.toString();
                logger.info("tcaPolicyReq=" + tcaPolicyReq);
                return tcaPolicyReq;
        }
        
        /**
         * Add appendSignatures to json
         * 
         * @param refProp
         * @param service
         * @param appendToNode
         * @param tca
         * @param prop
         * @throws JsonParseException
         * @throws JsonMappingException
         * @throws IOException
         */
        public static void appendSignatures(RefProp refProp, String service, ObjectNode appendToNode, Tca tca, ModelProperties prop) throws JsonParseException, JsonMappingException, IOException {
                //      "signatures":{
                ArrayNode tcaNodes = appendToNode.withArray("signatures");
                for(TcaItem tcaItem : tca.getTcaItems()){
                        ObjectNode tcaNode = (ObjectNode)refProp.getJsonTemplate("tca.signature.template", service);
                        tcaNode.put("useCaseName", tcaItem.getTcaName());
                        tcaNode.put("signatureName", tcaItem.getTcaName()+ "_" + tcaItem.getTcaUuId());
                        tcaNode.put("signatureUuid", tcaItem.getTcaUuId());
                        prop.setPolicyUniqueId(tcaItem.getPolicyId());
                        tcaNode.put("closedLoopControlName", prop.getControlNameAndPolicyUniqueId());
                        tcaNode.put("severity", tcaItem.getSeverity());
                        tcaNode.put("maxInterval", tcaItem.getInterval());
                        tcaNode.put("minMessageViolations", tcaItem.getViolations());
                        
                        tcaNodes.add(tcaNode);
                        Iterator<TcaThreshhold> scItr = tcaItem.getTcaThreshholds().iterator();
                        while(scItr.hasNext()) {
                                TcaThreshhold tcaThreshhold = scItr.next();
                                // "thresholds": [
                                ArrayNode thNodes = tcaNode.withArray("thresholds");
                                ObjectNode thNode = thNodes.addObject();
                                thNode.put("fieldPath", tcaThreshhold.getFieldPath());
                                thNode.put("thresholdName", tcaThreshhold.getMetric());
                                thNode.put("thresholdValue", tcaThreshhold.getThreshhold());
                                thNode.put("direction", tcaThreshhold.getOperator());
                        }               
                }
        }

}