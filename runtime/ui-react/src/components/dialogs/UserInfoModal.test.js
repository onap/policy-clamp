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
import UserInfoModal from './UserInfoModal';

describe('Verify UserInfoModal', () => {

  beforeEach(() => {
    fetch.resetMocks();
    fetch.mockImplementation(() => {
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => {
          return Promise.resolve({
            "userName": "test",
            "cldsVersion": "1.0.0"
          });
        }
      });
    });
  })

  it('Test the render method full permission', () => {
    const component = shallow(<UserInfoModal/>)
    component.setState({
      userInfo: {
        "userName": "test",
        "cldsVersion": "1.0.0",
        "allPermissions": ["permission1", "permission2"]
      }
    });
    expect(component).toMatchSnapshot();
  });

  it('Test the render method no permission', () => {
    const component = shallow(<UserInfoModal/>)
    component.setState({
      userInfo: {}
    });

    expect(component.find('FormControl').length).toEqual(0);
  });

  it('Test the render method read permission', () => {
    const component = shallow(<UserInfoModal/>)
    component.setState({
      userInfo: {
        "userName": "test",
        "cldsVersion": "1.0.0",
        "allPermissions": ["permission1", "permission2"]
      }
    });

    expect(component.find('FormControl').length).toEqual(4);

    const forms = component.find('FormControl');
    expect(forms.get(0).props.defaultValue).toEqual("test");
    expect(forms.get(1).props.defaultValue).toEqual("1.0.0");
    expect(forms.get(2).props.defaultValue).toEqual("permission1");
    expect(forms.get(3).props.defaultValue).toEqual("permission2");
  });
});
