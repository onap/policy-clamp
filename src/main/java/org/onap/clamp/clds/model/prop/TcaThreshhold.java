package org.onap.clamp.clds.model.prop;

import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Parse Tca Threshhold json properties.
 * 
 * Example json: {"TCA_0lm6cix":{"Narra":[{"name":"tname","value":"Narra"},{"name":"tcaEnab","value":"on"},{"name":"tcaPol","value":"Polcicy1"},{"name":"tcaPolId","value":"1"},{"name":"tcaInt","value":"1"},{"name":"tcaSev","value":"Critical"},{"name":"tcaVio","value":"1"},{"serviceConfigurations":[["FIELDPATH_test_1",">","4"],["FIELDPATH_test_1","=","5"]]}],"Srini":[{"name":"tname","value":"Srini"},{"name":"tcaEnab","value":"on"},{"name":"tcaPol","value":"Policy1"},{"name":"tcaPolId","value":"1"},{"name":"tcaInt","value":"1"},{"name":"tcaSev","value":"Major"},{"name":"tcaVio","value":"1"},{"serviceConfigurations":[["FIELDPATH_test_2","=","3"],["FIELDPATH_test_1",">","2"]]}]}}
 * 
 *
 */
public class TcaThreshhold {

	private static final Logger logger = Logger.getLogger(TcaThreshhold.class.getName());
	
	private String metric;
	private String fieldPath;
	private String operator;
	private Integer threshhold;
	
	/**
	 * Parse Tca Threshhold given json node
	 * 
	 * @param node
	 */
	public TcaThreshhold(JsonNode node) {
		
		if(node.get(0) != null){
			metric = node.get(0).asText();
		}
		if(node.get(1) != null){
			operator = node.get(1).asText();
		}
		if(node.get(2) != null){
			threshhold = Integer.valueOf(node.get(2).asText());
		}
		if(node.get(3) != null){
			fieldPath = node.get(3).asText();
		}
	}

	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public String getFieldPath() {
		return fieldPath;
	}

	public void setFieldPath(String fieldPath) {
		this.fieldPath = fieldPath;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public Integer getThreshhold() {
		return threshhold;
	}

	public void setThreshhold(Integer threshhold) {
		this.threshhold = threshhold;
	}
	
}
