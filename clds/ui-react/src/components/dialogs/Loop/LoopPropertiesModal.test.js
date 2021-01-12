/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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
import React from 'react';
import { shallow } from 'enzyme';
import LoopPropertiesModal from './LoopPropertiesModal';
import LoopCache from '../../../api/LoopCache';
import LoopService from '../../../api/LoopService';

describe('Verify LoopPropertiesModal', () => {
	const loopCache = new LoopCache({
		"name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca",
		"globalPropertiesJson": {
			"dcaeDeployParameters": {
				"location_id": "",
				"policy_id": "TCA_h2NMX_v1_0_ResourceInstanceName1_tca"
			}
		}
	});

	it('Test the render method', () => {
		const component = shallow(
			<LoopPropertiesModal loopCache={loopCache}/>
		)
		component.setState({ show: true,
			temporaryPropertiesJson: {
				"dcaeDeployParameters": {
					"location_id": "",
					"policy_id": "TCA_h2NMX_v1_0_ResourceInstanceName1_tca"
				}
			}
		});

	expect(component.state('temporaryPropertiesJson')).toEqual({
	"dcaeDeployParameters": {
		"location_id": "",
		"policy_id": "TCA_h2NMX_v1_0_ResourceInstanceName1_tca"}
	});
	expect(component.state('show')).toEqual(true);

	expect(component).toMatchSnapshot();
	});

	it('Test handleClose', () => {
		const historyMock = { push: jest.fn() };
		const handleClose = jest.spyOn(LoopPropertiesModal.prototype,'handleClose');
		const component = shallow(<LoopPropertiesModal history={historyMock} loopCache={loopCache}/>)

		component.find('[variant="secondary"]').prop('onClick')();

		expect(handleClose).toHaveBeenCalledTimes(1);
		expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
	});

	it('Test handleSave successful', async () => {
		const flushPromises = () => new Promise(setImmediate);
		const historyMock = { push: jest.fn() };
		const loadLoopFunction = jest.fn();
		const handleSave = jest.spyOn(LoopPropertiesModal.prototype,'handleSave');
		LoopService.updateGlobalProperties = jest.fn().mockImplementation(() => {
			return Promise.resolve({
				ok: true,
				status: 200,
				text: () => "OK"
			});
		});

		const component = shallow(<LoopPropertiesModal history={historyMock} 
						loopCache={loopCache} loadLoopFunction={loadLoopFunction} />)

		component.find('[variant="primary"]').prop('onClick')();
		await flushPromises();
		component.update();

		expect(handleSave).toHaveBeenCalledTimes(1);
		expect(component.state('show')).toEqual(false);
		expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
	});

	it('Onchange event', () => {
		const event = {target:{name:"dcaeDeployParameters", value:"{\"location_id\": \"testLocation\",\"policy_id\": \"TCA_h2NMX_v1_0_ResourceInstanceName1_tca\"}"}};
		const component = shallow(<LoopPropertiesModal loopCache={loopCache}/>);

		component.find('FormControl').simulate('change', event);
		component.update();

		expect(component.state('temporaryPropertiesJson').dcaeDeployParameters.location_id).toEqual("testLocation");
	});
});
