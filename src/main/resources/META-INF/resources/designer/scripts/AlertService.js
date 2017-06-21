/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

app.service('alertService', [function () {
	console.log("/////////////alertService");
	this.alertMessage=function(msg,typeNumber){
		var typeMessage="";
		switch(typeNumber){
			case 1:
				typeMessage="success";
				break;
			case 2:
				typeMessage="info";
				break;
			case 3:
				typeMessage="warning";
				break;
			case 4:
				typeMessage="danger";
				break;
			default:
				typeMessage="info";
		}
		$("#activity_modeler").prepend('<div id="alert_message_" style="postion:absolute;left:25;width:50%;" class="alert alert-'+typeMessage+'">'+msg+'</div>')
		setTimeout(function(){
		console.log("setTimeout");
		 $("#alert_message_").slideUp(500);}, 3000);

		
	}
 }]);
