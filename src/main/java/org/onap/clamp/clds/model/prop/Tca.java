package org.onap.clamp.clds.model.prop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Parse Tca json properties.
 * 
 * Example json: {"TCA_0lm6cix":{"Narra":[{"name":"tname","value":"Narra"},{"name":"tcaEnab","value":"on"},{"name":"tcaPol","value":"Polcicy1"},{"name":"tcaPolId","value":"1"},{"name":"tcaInt","value":"1"},{"name":"tcaSev","value":"Critical"},{"name":"tcaVio","value":"1"},{"serviceConfigurations":[["FIELDPATH_test_1",">","4"],["FIELDPATH_test_1","=","5"]]}],"Srini":[{"name":"tname","value":"Srini"},{"name":"tcaEnab","value":"on"},{"name":"tcaPol","value":"Policy1"},{"name":"tcaPolId","value":"1"},{"name":"tcaInt","value":"1"},{"name":"tcaSev","value":"Major"},{"name":"tcaVio","value":"1"},{"serviceConfigurations":[["FIELDPATH_test_2","=","3"],["FIELDPATH_test_1",">","2"]]}]}}
 * 
 *
 */
public class Tca extends ModelElement {
	
    private static final Logger logger = Logger.getLogger(StringMatch.class.getName());
	
	private List<TcaItem> tcaItems;

	/**
	 * Parse Tca given json node
	 * 
	 * @param modelProp
	 * @param modelBpmn
	 * @param modelJson
	 */
	public Tca(ModelProperties modelProp, ModelBpmn modelBpmn, JsonNode modelJson) {
		super(ModelElement.TYPE_TCA, modelProp, modelBpmn, modelJson);
		
		// process Server_Configurations
		if(meNode != null){
			Iterator<JsonNode> itr = meNode.elements();
			tcaItems = new ArrayList<TcaItem>();
			while(itr.hasNext()) {
				tcaItems.add(new TcaItem(itr.next()));
			}
		}
	}

	public List<TcaItem> getTcaItems() {
		return tcaItems;
	}

}
