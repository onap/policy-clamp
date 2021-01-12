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
import DeployLoopModal from './DeployLoopModal';
import LoopCache from '../../../api/LoopCache';
import LoopActionService from '../../../api/LoopActionService';
import LoopService from '../../../api/LoopService';

describe('Verify DeployLoopModal', () => {
	const loopCache = new LoopCache({
		"name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca",
		"globalPropertiesJson": {
			"dcaeDeployParameters": {
				"testMs": {
					"location_id": "",
					"policy_id": "TCA_h2NMX_v1_0_ResourceInstanceName1_tca"
				}
			}
		}
	});

	it('Test the render method', () => {
		const component = shallow(
			<DeployLoopModal loopCache={loopCache}/>
		)

	expect(component).toMatchSnapshot();
	});
	
	it('Test handleClose', () => {
		const historyMock = { push: jest.fn() };
		const handleClose = jest.spyOn(DeployLoopModal.prototype,'handleClose');
		const component = shallow(<DeployLoopModal history={historyMock} loopCache={loopCache}/>)

		component.find('[variant="secondary"]').prop('onClick')();

		expect(handleClose).toHaveBeenCalledTimes(1);
		expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
	});

	it('Test handleSave successful', async () => {
		const flushPromises = () => new Promise(setImmediate);
		const historyMock = { push: jest.fn() };
		const updateLoopFunction = jest.fn();
		const showSucAlert = jest.fn();
		const showFailAlert = jest.fn();
		const handleSave = jest.spyOn(DeployLoopModal.prototype,'handleSave');
		LoopService.updateGlobalProperties = jest.fn().mockImplementation(() => {
			return Promise.resolve({
				ok: true,
				status: 200,
				text: () => "OK"
			});
		});
		LoopActionService.performAction = jest.fn().mockImplementation(() => {
			return Promise.resolve({
				ok: true,
				status: 200,
				json: () => {}
			});
		});
		LoopActionService.refreshStatus = jest.fn().mockImplementation(() => {
			return Promise.resolve({
				ok: true,
				status: 200,
				json: () => {}
			});
		});

		const component = shallow(<DeployLoopModal history={historyMock} 
						loopCache={loopCache} updateLoopFunction={updateLoopFunction} showSucAlert={showSucAlert} showFailAlert={showFailAlert} />)

		component.find('[variant="primary"]').prop('onClick')();
		await flushPromises();
		component.update();

		expect(handleSave).toHaveBeenCalledTimes(1);
		expect(component.state('show')).toEqual(false);
		expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
		handleSave.mockClear();
	});

	it('Onchange event', () => {
		const event = { target: { name: "location_id", value: "testLocation"} };
		const component = shallow(<DeployLoopModal loopCache={loopCache}/>);

		component.find('[name="location_id"]').simulate('change', event);
		component.update();
		expect(component.state('temporaryPropertiesJson').dcaeDeployParameters.testMs.location_id).toEqual("testLocation");
	});
});