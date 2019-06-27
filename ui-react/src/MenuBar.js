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
import Navbar from 'react-bootstrap/Navbar';
import NavDropdown from 'react-bootstrap/NavDropdown';
import 'bootstrap-css-only/css/bootstrap.min.css';

class MenuBar extends React.Component {
	render () {
	   return (
  <Navbar.Collapse id="basic-navbar-nav" className="justify-content-center">
      <NavDropdown title="Closed Loop" id="basic-nav-dropdown">
        <NavDropdown.Item href="#action/3.1">Open CL</NavDropdown.Item>
        <NavDropdown.Item href="#action/3.2">Properties CL</NavDropdown.Item>
        <NavDropdown.Item href="#action/3.3">Close Model</NavDropdown.Item>
      </NavDropdown>
			<NavDropdown title="Manage" id="basic-nav-dropdown">
				<NavDropdown.Item href="#action/3.1">Submit</NavDropdown.Item>
				<NavDropdown.Item href="#action/3.2">Stop</NavDropdown.Item>
				<NavDropdown.Item href="#action/3.3">Restart</NavDropdown.Item>
				<NavDropdown.Item href="#action/3.3">Delete</NavDropdown.Item>
				<NavDropdown.Item href="#action/3.3">Deploy</NavDropdown.Item>
				<NavDropdown.Item href="#action/3.3">UnDeploy</NavDropdown.Item>
			</NavDropdown>
			<NavDropdown title="View" id="basic-nav-dropdown">
				<NavDropdown.Item href="#action/3.1">Refresh Status</NavDropdown.Item>
			</NavDropdown>
		<NavDropdown title="Help" id="basic-nav-dropdown">
			<NavDropdown.Item href="#action/3.1">Wiki</NavDropdown.Item>
			<NavDropdown.Item href="#action/3.2">Contact Us</NavDropdown.Item>
			<NavDropdown.Item href="#action/3.3">User Info</NavDropdown.Item>
		</NavDropdown>
  </Navbar.Collapse>


    );
  }
}



export default MenuBar;
