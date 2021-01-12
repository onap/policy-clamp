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
import LoopLogs from './LoopLogs';
import LoopCache from '../../../api/LoopCache';

describe('Verify LoopLogs', () => {

	const loopCache = new LoopCache({
		"name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca",
			"loopLogs": [
			{
				"id": 1,
				"logType": "INFO",
				"logComponent": "CLAMP",
				"message": "Operational policies UPDATED",
				"logInstant": "2019-07-08T09:44:37Z"
			}
		]
	});

	it('Test the render method', () => {
		const component = shallow(<LoopLogs loopCache={loopCache}/>)
		expect(component).toMatchSnapshot();

		const loopCacheUpdated = new LoopCache({
		"name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca",
			"loopLogs": [
			{
				"id": 1,
				"logType": "INFO",
				"logComponent": "CLAMP",
				"message": "Operational policies UPDATED",
				"logInstant": "2019-07-08T09:44:37Z"
			},
			{
				"id": 2,
				"logType": "INFO",
				"logComponent": "CLAMP",
				"message": "Operational policies UPDATED",
				"logInstant": "2019-07-08T09:44:50Z"
			}
			]
		});

		component.setProps({ loopCache: loopCacheUpdated });
		expect(component.find('TableRow').length).toEqual(2);
	});
});