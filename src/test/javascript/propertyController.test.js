
describe('Property controller tests', function() {
	var clModel = '{"name": "ClosedLoopTest","dcaeDeploymentId":"testId","dcaeDeploymentStatusUrl":"testUrl","lastComputedState":"DESIGN","svgRepresentation": "representation","globalPropertiesJson": [{"name":"deployParameters","value":{"location_id":"","service_id":"","policy_id":"AUTO_GENERATED_POLICY_ID_AT_SUBMIT"}}], "blueprint": "yaml","lastComputedState": "DESIGN","operationalPolicies": [ {"name": "OpPolicyTest", "configurationsJson": { "policy1": [{"name": "pname","value": "policy1"}]}}],"microServicePolicies": [{"name": "tca","properties": "", "shared": true,"policyTosca": "tosca","jsonRepresentation": {"schema":{"title":"DCAE TCA Config","type":"object","required":["name"],"properties":{"name":{"propertyOrder":101,"title":"Name","type":"string"}}}}}],"loopLogs": [{ } ] }';
	cl_props = JSON.parse(clModel);
	var propertyController = require('scripts/propertyController.js');
	
	test('getOperationalPolicyProperty', () => {
		var policyProp = '{"policy1": [{"name": "pname","value": "policy1"}]}';
		expect(propertyController.getOperationalPolicyProperty()).toEqual(JSON.parse(policyProp));
	});

	test('getGlobalProperty', () => {
		var globalProp = '[{"name":"deployParameters","value":{"location_id":"","service_id":"","policy_id":"AUTO_GENERATED_POLICY_ID_AT_SUBMIT"}}]';
		expect(propertyController.getGlobalProperty()).toEqual(JSON.parse(globalProp));
	});

	test('getMsPropertyTca', () => {
		expect(propertyController.getMsProperty("tca")).toEqual('');
	});

	test('getMsUITca', () => {
		var msUI = '{"schema":{"title":"DCAE TCA Config","type":"object","required":["name"],"properties":{"name":{"propertyOrder":101,"title":"Name","type":"string"}}}}';
		expect(propertyController.getMsUI("tca")).toEqual(JSON.parse(msUI));
	});

	test('getMsPropertyNotExist', () => {
		  expect(propertyController.getMsProperty("test")).toEqual(null);
	});

	test('getMsUINotExist', () => {
		  expect(propertyController.getMsUI("test")).toEqual(null);
	});

	test('getStatus', () => {
		  expect(propertyController.getStatus()).toEqual('DESIGN');
	});

	test('getDeploymentID', () => {
		  expect(propertyController.getDeploymentID()).toEqual('testId');
	});

	test('getDeploymentStatusURL', () => {
		  expect(propertyController.getDeploymentStatusURL()).toEqual('testUrl');
	});
});