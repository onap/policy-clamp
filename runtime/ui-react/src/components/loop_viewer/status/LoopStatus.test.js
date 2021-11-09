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
import LoopStatus from './LoopStatus';
import LoopCache from '../../../api/LoopCache';

describe('Verify LoopStatus', () => {

  const loopCache = new LoopCache({
    "name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca",
    "lastComputedState": "DESIGN",
    "components": {
      "POLICY": {
        "componentState": {
          "stateName": "NOT_SENT",
          "description": "The policies defined have NOT yet been created on the policy engine"
        }
      },
      "DCAE": {
        "componentState": {
          "stateName": "BLUEPRINT_DEPLOYED",
          "description": "The DCAE blueprint has been found in the DCAE inventory but not yet instancianted for this loop"
        }
      }
    }
  });

  it('Test the render method', () => {
    const component = shallow(<LoopStatus loopCache={ loopCache }/>)

    expect(component).toMatchSnapshot();

    const loopCacheUpdated = new LoopCache({
      "name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca",
      "lastComputedState": "SUBMIT",
      "components": {
        "POLICY": {
          "componentState": {
            "stateName": "SENT",
            "description": "The policies defined have NOT yet been created on the policy engine"
          }
        },
        "DCAE": {
          "componentState": {
            "stateName": "BLUEPRINT_DEPLOYED",
            "description": "The DCAE blueprint has been found in the DCAE inventory but not yet instancianted for this loop"
          }
        }
      }
    });
    component.setProps({ loopCache: loopCacheUpdated });

    const forms = component.find('TableRow');
    expect(forms.get(0).props.statusRow.stateName).toEqual("SENT");
    expect(component.find('label').text()).toContain('SUBMIT');
  });
});
