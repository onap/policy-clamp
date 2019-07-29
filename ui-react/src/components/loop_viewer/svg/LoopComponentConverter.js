export default class LoopComponentConverter {

	static buildMapOfComponents(loopCache) {
		var componentsMap = new Map([]);
		if (typeof (loopCache.getMicroServicePolicies()) !== "undefined") {
			loopCache.getMicroServicePolicies().forEach(ms => {
				componentsMap.set(ms.name, "/configurationPolicyModal/"+ms.name);
			})
		}
		if (typeof (loopCache.getOperationalPolicies()) !== "undefined") {
			loopCache.getOperationalPolicies().forEach(op => {
				componentsMap.set(op.name, "/operationalPolicyModal");
			})
		}
		componentsMap.set("OperationalPolicy","/operationalPolicyModal");
		return componentsMap;
	}
}
