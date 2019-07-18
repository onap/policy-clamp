export default class LoopComponentConverter {

	static buildMapOfComponents(loopCache) {
		var componentsMap = new Map([]);
		if (typeof (loopCache.getMicroServicePolicies()) !== "undefined") {
			loopCache.getMicroServicePolicies().map(ms => {
				componentsMap.set(ms.name, "/configurationPolicyModal/"+ms.name);
			})
		}
		if (typeof (loopCache.getOperationalPolicies()) !== "undefined") {
			loopCache.getOperationalPolicies().map(op => {
				componentsMap.set(op.name, "/operationalPolicyModal");
			})
		}
		componentsMap.set("OperationalPolicy","/operationalPolicyModal");
		return componentsMap;
	}
}