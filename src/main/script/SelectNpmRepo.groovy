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
 * 
 */

package org.onap.clamp.clds.maven.scripts

println project.properties['clamp.project.version'];

if ( project.properties['clamp.project.version'].endsWith("-SNAPSHOT") ) {
    project.properties['npm.publish.url']="https://nexus3.onap.org/repository/npm.snapshot/"
} else {
    project.properties['npm.publish.url']="https://nexus3.onap.org/repository/npm.release/"
} 

println 'NPM repository: ' + project.properties['npm.publish.url'];