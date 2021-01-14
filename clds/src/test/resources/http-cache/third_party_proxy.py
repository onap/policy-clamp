#!/usr/bin/env python2
###
# ============LICENSE_START=======================================================
# ONAP CLAMP
# ================================================================================
# Copyright (C) 2018 AT&T Intellectual Property. All rights
#                             reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END============================================
# ===================================================================
#
###

import json
import requests
import os
import errno
import sys
import SimpleHTTPServer
import SocketServer
import argparse
import tempfile
import signal
import uuid
import shutil

parser = argparse.ArgumentParser(description="3rd party Cache & Replay")
parser.add_argument("--username", "-u", type=str, help="Set the username for contacting 3rd party - only used for GET")
parser.add_argument("--password", "-p", type=str, help="Set the password for contacting 3rd party - only used for GET")
parser.add_argument("--root",     "-r", default=tempfile.mkdtemp(), type=str, help="Root folder for the proxy cache")
parser.add_argument("--temp",     "-t", default=tempfile.mkdtemp(), type=str, help="Temp folder for the generated content")
parser.add_argument("--proxy"         , type=str, help="Url of the  Act as a proxy. If not set, this script only uses the cache and will return a 404 if files aren't found")
parser.add_argument("--port",     "-P", type=int, default="8081", help="Port on which the proxy should listen to")
parser.add_argument("--verbose",  "-v", type=bool, help="Print more information in case of error")
parser.add_argument("--proxyaddress","-a", type=str, help="Address of this proxy, generally either third-party-proxy:8085 or localhost:8085 depending if started with docker-compose or not")
options = parser.parse_args()


PORT = options.port
HOST = options.proxy
AUTH = (options.username, options.password)
HEADERS = {'X-ECOMP-InstanceID':'CLAMP'}
CACHE_ROOT = str(options.root)
TMP_ROOT = str(options.temp)
PROXY_ADDRESS=str(options.proxyaddress)

def signal_handler(signal_sent, frame):
    global httpd
    if signal_sent == signal.SIGINT:
        print('Got Ctrl-C (SIGINT)')
        httpd.socket.close()
        httpd.shutdown()
        httpd.server_close()

class Proxy(SimpleHTTPServer.SimpleHTTPRequestHandler):
    def print_headers(self):
        for header,value in self.headers.items():
            print("header: %s : %s" % (header, value))

    def check_credentials(self):
        pass

    def _send_content(self, header_file, content_file):
        self.send_response(200)
        with open(header_file, 'rb') as f:
            headers = json.load(f)
            for key,value in headers.items():
                if key in ('Transfer-Encoding',):
                    continue
                self.send_header(key, value)
            self.end_headers()
        with open(content_file,'rb') as f:
            fc = f.read()
            self.wfile.write(fc)

    def _write_cache(self,cached_file_folder, header_file, content_file, response):
        os.makedirs(cached_file_folder, 0777)
        with open(content_file, 'w') as f:
            f.write(response.raw.read())
        with open(header_file, 'w') as f:
            json.dump(dict(response.raw.headers), f)
    # Entry point of the code
    def _get_cached_file_folder_name(self,folder):
        cached_file_folder = '%s/%s' % (folder, self.path,)
        print("Cached file name before escaping : %s" % cached_file_folder)
        cached_file_folder = cached_file_folder.replace('<','&#60;').replace('>','&#62;').replace('?','&#63;').replace('*','&#42;').replace('\\','&#42;').replace(':','&#58;').replace('|','&#124;')
        print("Cached file name after escaping (used for cache storage) : %s" % cached_file_folder)
        return cached_file_folder
    
    def _get_cached_content_file_name(self,cached_file_folder):
        return "%s/.file" % (cached_file_folder,)
    
    def _get_cached_header_file_name(self,cached_file_folder):
        return "%s/.header" % (cached_file_folder,)
    
    def _execute_content_generated_cases(self,http_type):
     print("Testing special cases, cache files will be sent to :" +TMP_ROOT)
     cached_file_folder = self._get_cached_file_folder_name(TMP_ROOT)
     cached_file_content = self._get_cached_content_file_name(cached_file_folder)
     cached_file_header = self._get_cached_header_file_name(cached_file_folder)
     _file_available = os.path.exists(cached_file_content)
    
     if self.path.startswith("/dcae-service-types?asdcResourceId=") and http_type == "GET":
        if not _file_available:
            print "self.path start with /dcae-service-types?asdcResourceId=, generating response json..."
            uuidGenerated = str(uuid.uuid4())
            typeId = "typeId-" + uuidGenerated
            typeName = "typeName-" + uuidGenerated
            print "typeId generated: " + typeName + " and typeName: "+ typeId
            jsonGenerated = "{\"totalCount\":1, \"items\":[{\"typeId\":\"" + typeId + "\", \"typeName\":\"" + typeName +"\"}]}"
            print "jsonGenerated: " + jsonGenerated
    
            os.makedirs(cached_file_folder, 0777)
            with open(cached_file_header, 'w') as f:
                f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
            with open(cached_file_content, 'w') as f:
                f.write(jsonGenerated)
        return True
     elif self.path.startswith("/dcae-operationstatus/install") and http_type == "GET":
        if not _file_available:
            print "self.path start with /dcae-operationstatus/install, generating response json..."
            jsonGenerated =  "{\"operationType\": \"install\", \"status\": \"succeeded\"}"
            print "jsonGenerated: " + jsonGenerated
    
            try:
                os.makedirs(cached_file_folder, 0777)
            except OSError as e:
                if e.errno != errno.EEXIST:
                    raise
                print(cached_file_folder+" already exists")
    
            with open(cached_file_header, 'w') as f:
                f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
            with open(cached_file_content, 'w') as f:
                f.write(jsonGenerated)
        return True
     elif self.path.startswith("/dcae-operationstatus/uninstall") and http_type == "GET":
        if not _file_available:
            print "self.path start with /dcae-operationstatus/uninstall, generating response json..."
            jsonGenerated =  "{\"operationType\": \"uninstall\", \"status\": \"succeeded\"}"
            print "jsonGenerated: " + jsonGenerated
    
            try:
                os.makedirs(cached_file_folder, 0777)
            except OSError as e:
                if e.errno != errno.EEXIST:
                    raise
                print(cached_file_folder+" already exists")
    
            with open(cached_file_header, 'w') as f:
                f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
            with open(cached_file_content, 'w') as f:
                f.write(jsonGenerated)
        return True
     elif self.path.startswith("/sdc/v1/catalog/services/") and http_type == "POST":
        if not _file_available:
            print "self.path start with /sdc/v1/catalog/services/, generating response json..."
            jsondata = json.loads(self.data_string)
            jsonGenerated = "{\"artifactName\":\"" + jsondata['artifactName'] + "\",\"artifactType\":\"" + jsondata['artifactType'] + "\",\"artifactURL\":\"" + self.path + "\",\"artifactDescription\":\"" + jsondata['description'] + "\",\"artifactChecksum\":\"ZjJlMjVmMWE2M2M1OTM2MDZlODlmNTVmZmYzNjViYzM=\",\"artifactUUID\":\"" + str(uuid.uuid4()) + "\",\"artifactVersion\":\"1\"}"
            print "jsonGenerated: " + jsonGenerated
    
            os.makedirs(cached_file_folder, 0777)
            with open(cached_file_header, 'w') as f:
                f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
            with open(cached_file_content, 'w') as f:
                f.write(jsonGenerated)
        return True
     elif self.path.startswith("/dcae-deployments/") and http_type == "PUT":
            print "self.path start with /dcae-deployments/ DEPLOY, generating response json..."
            #jsondata = json.loads(self.data_string)
            jsonGenerated = "{\"operationType\":\"install\",\"status\":\"processing\",\"links\":{\"status\":\"http:\/\/" + PROXY_ADDRESS + "\/dcae-operationstatus/install?test=test\"}}"
            print "jsonGenerated: " + jsonGenerated
            if not os.path.exists(cached_file_folder):
                os.makedirs(cached_file_folder, 0777)
            with open(cached_file_header, 'w+') as f:
                f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
            with open(cached_file_content, 'w+') as f:
                f.write(jsonGenerated)
        	return True
     elif self.path.startswith("/dcae-deployments/") and http_type == "DELETE":
            print "self.path start with /dcae-deployments/ UNDEPLOY, generating response json..."
            #jsondata = json.loads(self.data_string)
            jsonGenerated = "{\"operationType\":\"uninstall\",\"status\":\"processing\",\"links\":{\"status\":\"http:\/\/" + PROXY_ADDRESS + "\/dcae-operationstatus/uninstall?test=test\"}}"
            print "jsonGenerated: " + jsonGenerated
            if not os.path.exists(cached_file_folder):
                os.makedirs(cached_file_folder, 0777)
            with open(cached_file_header, 'w+') as f:
                f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
            with open(cached_file_content, 'w+') as f:
                f.write(jsonGenerated)
            return True
     elif (self.path.startswith("/pdp/api/") and (http_type == "PUT" or http_type == "DELETE")) or (self.path.startswith("/pdp/api/policyEngineImport") and http_type == "POST"):
        print "self.path start with /pdp/api/, copying body to response ..."
        if not os.path.exists(cached_file_folder):
            os.makedirs(cached_file_folder, 0777)
        with open(cached_file_header, 'w+') as f:
            f.write("{\"Content-Length\": \"" + str(len(self.data_string)) + "\", \"Content-Type\": \""+str(self.headers['Content-Type'])+"\"}")
        with open(cached_file_content, 'w+') as f:
            f.write(self.data_string)
        return True
     elif self.path.startswith("/policy/api/v1/policytypes/") and http_type == "POST":
        print "self.path start with POST new policy API /pdp/api/, copying body to response ..."
        if not os.path.exists(cached_file_folder):
            os.makedirs(cached_file_folder, 0777)
        with open(cached_file_header, 'w+') as f:
            f.write("{\"Content-Length\": \"" + str(len(self.data_string)) + "\", \"Content-Type\": \""+str(self.headers['Content-Type'])+"\"}")
        with open(cached_file_content, 'w+') as f:
            f.write(self.data_string)
        return True
     elif self.path.startswith("/policy/api/v1/policytypes/") and http_type == "DELETE":
        print "self.path start with DELETE new policy API /policy/api/v1/policytypes/ ..."
        if not os.path.exists(cached_file_folder):
            os.makedirs(cached_file_folder, 0777)
    
        with open(cached_file_header, 'w+') as f:
                f.write("{\"Content-Length\": \"" + str(len("")) + "\", \"Content-Type\": \""+str("")+"\"}")
        with open(cached_file_content, 'w+') as f:
                f.write(self.data_string)
        return True
     elif self.path.startswith("/policy/pap/v1/pdps/policies") and http_type == "POST":
        print "self.path start with POST new policy API /policy/pap/v1/pdps/ ..."
        if not os.path.exists(cached_file_folder):
            os.makedirs(cached_file_folder, 0777)
        with open(cached_file_header, 'w+') as f:
                f.write("{\"Content-Length\": \"" + str(len("")) + "\", \"Content-Type\": \""+str("")+"\"}")
        with open(cached_file_content, 'w+') as f:
                f.write(self.data_string)
        return True
     elif (self.path.startswith("/policy/pap/v1/policies/deployed/")) and http_type == "GET":
        print "self.path start with /policy/api/v1/policytypes/, generating response json..."
        jsonGenerated =  "{\"policyTypeId\": \"onap.policies.controlloop.operational\",\"policyTypeVersion\": \"1.0.0\",\"policyId\": \"OPERATIONAL_z711F_v1_0_ResourceInstanceName1_tca\"}"
        print "jsonGenerated: " + jsonGenerated
        if not os.path.exists(cached_file_folder):
            os.makedirs(cached_file_folder, 0777)

        with open(cached_file_header, 'w') as f:
            f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
        with open(cached_file_content, 'w') as f:
            f.write(jsonGenerated)
        return True
     elif self.path.startswith("/dcae-service-types") and http_type == "GET":
        if not _file_available:
            self.path = "/dcae-service-types"
            cached_file_folder = '%s/%s' % (TMP_ROOT, self.path)
            cached_file_content = self._get_cached_content_file_name(cached_file_folder)
            cached_file_header = self._get_cached_header_file_name(cached_file_folder)
            print "self.path start with /dcae-service-types, generating response json..."
            response = "{\"links\": {\"previousLink\": {\"title\": \"string\",\"rel\": \"string\",\"uri\": \"string\",\"uriBuilder\": {},\"rels\": [\"string\"],\"params\": {\"additionalProp1\": \"string\",\"additionalProp2\": \"string\",\"additionalProp3\": \"string\"},\"type\": \"string\"},\"nextLink\": {\"title\": \"string\",\"rel\": \"string\",\"uri\": \"string\",\"uriBuilder\": {},\"rels\": [\"string\"],\"params\": {\"additionalProp1\": \"string\",\"additionalProp2\": \"string\",\"additionalProp3\": \"string\"},\"type\": \"string\"}},\"totalCount\": 1,\"items\": [{\"owner\": \"testOwner\",\"application\": \"testApplication\",\"component\": \"testComponent\",\"typeName\": \"testTypeName\",\"typeVersion\": 0,\"blueprintTemplate\": \"testBlueprintTemplate\",\"serviceIds\": [\"serviceId1\", \"serviceId2\"],\"vnfTypes\": [\"vnfType1\", \"vnfType2\"],\"serviceLocations\": [\"serviceLocation1\", \"serviceLocation2\"],\"asdcServiceId\": \"testAsdcServiceId\",\"asdcResourceId\": \"0\",\"asdcServiceURL\": \"testAsdcServiceURL\",\"typeId\": \"testtypeId\",\"selfLink\": {\"title\": \"selfLinkTitle\",\"rel\": \"selfLinkRel\",\"uri\": \"selfLinkUri\",\"uriBuilder\": {},\"rels\": [\"string\"],\"params\": {\"additionalProp1\": \"string\",\"additionalProp2\": \"string\",\"additionalProp3\": \"string\"},\"type\": \"string\"},\"created\": \"2020-01-22T09:38:15.436Z\",\"deactivated\": \"2020-01-22T09:38:15.437Z\"},{\"owner\": \"testOwner2\",\"application\": \"testApplication1\",\"component\": \"testComponent2\",\"typeName\": \"testTypeName2\",\"typeVersion\": 0,\"blueprintTemplate\": \"testBlueprintTemplate2\",\"serviceIds\": [\"serviceId3\", \"serviceId4\"],\"vnfTypes\": [\"vnfType13\", \"vnfType4\"],\"serviceLocations\": [\"serviceLocation3\", \"serviceLocation4\"],\"asdcServiceId\": \"testAsdcServiceId\",\"asdcResourceId\": \"1\",\"asdcServiceURL\": \"testAsdcServiceURL2\",\"typeId\": \"testtypeId2\",\"selfLink\": {\"title\": \"selfLinkTitle\",\"rel\": \"selfLinkRel\",\"uri\": \"selfLinkUri\",\"uriBuilder\": {},\"rels\": [\"string\"],\"params\": {\"additionalProp1\": \"string\",\"additionalProp2\": \"string\",\"additionalProp3\": \"string\"},\"type\": \"string\"},\"created\": \"2020-01-22T09:38:15.436Z\",\"deactivated\": \"2020-01-22T09:38:15.437Z\"}]}"
            print "jsonGenerated: " + response

            os.makedirs(cached_file_folder, 0777)
            with open(cached_file_header, 'w') as f:
                f.write("{\"Content-Length\": \"" + str(len(response)) + "\", \"Content-Type\": \"application/json\"}")
            with open(cached_file_content, 'w') as f:
                f.write(response)
        return True
     else:
        return False

    
    def do_GET(self):
        cached_file_folder = ""
        cached_file_content =""
        cached_file_header=""
        print("\n\n\nGot a GET request for %s " % self.path)

        self.print_headers()
        self.check_credentials()
        # Verify if it's a special case
        is_special = self._execute_content_generated_cases("GET")
        if is_special:
            cached_file_folder = self._get_cached_file_folder_name(TMP_ROOT)
            cached_file_content = self._get_cached_content_file_name(cached_file_folder)
            cached_file_header = self._get_cached_header_file_name(cached_file_folder)
        else:
            cached_file_folder = self._get_cached_file_folder_name(CACHE_ROOT)
            cached_file_content = self._get_cached_content_file_name(cached_file_folder)
            cached_file_header = self._get_cached_header_file_name(cached_file_folder)

        _file_available = os.path.exists(cached_file_content)

        if not _file_available:
            print("Request for data currently not present in cache: %s" % (cached_file_folder,))

            if not HOST:
                self.send_response(404)
                self.end_headers()
                self.wfile.write('404 Not found, no remote HOST specified on the emulator !!!')
                print("HOST value is: %s " % (options.proxy))
                return "404 Not found, no remote HOST specified on the emulator !!!"

            url = '%s%s' % (HOST, self.path)
            response = requests.get(url, auth=AUTH, headers=HEADERS, stream=True)

            if response.status_code == 200:
                self._write_cache(cached_file_folder, cached_file_header, cached_file_content, response)
            else:
                print('Error when requesting file :')
                print('Requested url : %s' % (url,))
                print('Status code : %s' % (response.status_code,))
                print('Content : %s' % (response.content,))
                self.send_response(response.status_code)
                self.end_headers()
                self.wfile.write('404 Not found, nothing found on the remote server !!!')
                return response.content
        else:
            print("Request for data currently present in cache: %s" % (cached_file_folder,))
            print("HOST value is: %s " % (HOST))

        self._send_content(cached_file_header, cached_file_content)

        if self.path.startswith("/dcae-service-types?asdcResourceId="):
            print "DCAE case deleting folder created " + cached_file_folder
            shutil.rmtree(cached_file_folder, ignore_errors=False, onerror=None)
        elif self.path.startswith("/dcae-service-types"):
            print "DCAE case deleting folder created " + cached_file_folder
            shutil.rmtree(cached_file_folder, ignore_errors=False, onerror=None)
        else:
            print "NOT in DCAE case deleting folder created " + cached_file_folder

    def do_POST(self):
        cached_file_folder = ""
        cached_file_content =""
        cached_file_header=""
        print("\n\n\nGot a POST for %s" % self.path)
        self.check_credentials()
        self.data_string = self.rfile.read(int(self.headers['Content-Length']))
        print("data-string:\n %s" % self.data_string)
        print("self.headers:\n %s" % self.headers)

        is_special = self._execute_content_generated_cases("POST")
        if is_special:
            cached_file_folder = self._get_cached_file_folder_name(TMP_ROOT)
            cached_file_content = self._get_cached_content_file_name(cached_file_folder)
            cached_file_header = self._get_cached_header_file_name(cached_file_folder)
        else:
            cached_file_folder = self._get_cached_file_folder_name(CACHE_ROOT)
            cached_file_content = self._get_cached_content_file_name(cached_file_folder)
            cached_file_header = self._get_cached_header_file_name(cached_file_folder)

        _file_available = os.path.exists(cached_file_content)

        if not _file_available:
        
            if not HOST:
                self.send_response(404)
                self.end_headers()
                self.wfile.write('404 Not found, no remote HOST specified on the emulator !!!')
                return "404 Not found, no remote HOST specified on the emulator !!!"

            print("Request for data currently not present in cache: %s" % (cached_file_folder,))

            url = '%s%s' % (HOST, self.path)
            print("url: %s" % (url,))
            response = requests.post(url, data=self.data_string, headers=self.headers, stream=True)

            if response.status_code == 200:
                self._write_cache(cached_file_folder, cached_file_header, cached_file_content, response)
            else:
                print('Error when requesting file :')
                print('Requested url : %s' % (url,))
                print('Status code : %s' % (response.status_code,))
                print('Content : %s' % (response.content,))
                self.send_response(response.status_code)
                self.end_headers()
                self.wfile.write('404 Not found, nothing found on the remote server !!!')
                return response.content
        else:
            print("Request for data present in cache: %s" % (cached_file_folder,))

        self._send_content(cached_file_header, cached_file_content)

    def do_PUT(self):
        cached_file_folder = ""
        cached_file_content =""
        cached_file_header=""
        print("\n\n\nGot a PUT for %s " % self.path)
        self.check_credentials()
        self.data_string = self.rfile.read(int(self.headers['Content-Length']))
        print("data-string:\n %s" % self.data_string)
        print("self.headers:\n %s" % self.headers)

        is_special = self._execute_content_generated_cases("PUT")
        if is_special:
            cached_file_folder = self._get_cached_file_folder_name(TMP_ROOT)
            cached_file_content = self._get_cached_content_file_name(cached_file_folder)
            cached_file_header = self._get_cached_header_file_name(cached_file_folder)
        else:
            cached_file_folder = self._get_cached_file_folder_name(CACHE_ROOT)
            cached_file_content = self._get_cached_content_file_name(cached_file_folder)
            cached_file_header = self._get_cached_header_file_name(cached_file_folder)

        _file_available = os.path.exists(cached_file_content)

        if not _file_available:
            if not HOST:
                self.send_response(404)
                self.end_headers()
                self.wfile.write('404 Not found, no remote HOST specified on the emulator !!!')
                return "404 Not found, no remote HOST specified on the emulator !!!"

            print("Request for data currently not present in cache: %s" % (cached_file_folder,))

            url = '%s%s' % (HOST, self.path)
            print("url: %s" % (url,))
            response = requests.put(url, data=self.data_string, headers=self.headers, stream=True)

            if response.status_code == 200:
                self._write_cache(cached_file_folder, cached_file_header, cached_file_content, response)
            else:
                print('Error when requesting file :')
                print('Requested url : %s' % (url,))
                print('Status code : %s' % (response.status_code,))
                print('Content : %s' % (response.content,))
                self.send_response(response.status_code)
                self.end_headers()
                self.wfile.write('404 Not found, nothing found on the remote server !!!')
                return response.content
        else:
            print("Request for data present in cache: %s" % (cached_file_folder,))

        self._send_content(cached_file_header, cached_file_content)


    def do_DELETE(self):
        cached_file_folder = ""
        cached_file_content =""
        cached_file_header=""
        print("\n\n\nGot a DELETE for %s " % self.path)
        self.check_credentials()
        if self.headers.get('Content-Length') is not None:
            self.data_string = self.rfile.read(int(self.headers['Content-Length']))
        else:
            self.data_string = "empty generated"
        print("self.headers:\n %s" % self.headers)

        is_special = self._execute_content_generated_cases("DELETE")
        if is_special:
            cached_file_folder = self._get_cached_file_folder_name(TMP_ROOT)
            cached_file_content = self._get_cached_content_file_name(cached_file_folder)
            cached_file_header = self._get_cached_header_file_name(cached_file_folder)
        else:
            cached_file_folder = self._get_cached_file_folder_name(CACHE_ROOT)
            cached_file_content = self._get_cached_content_file_name(cached_file_folder)
            cached_file_header = self._get_cached_header_file_name(cached_file_folder)

        _file_available = os.path.exists(cached_file_content)

        if not _file_available:
            if not HOST:
                self.send_response(404)
                self.end_headers()
                self.wfile.write('404 Not found, no remote HOST specified on the emulator !!!')
                return "404 Not found, no remote HOST specified on the emulator !!!"

            print("Request for data currently not present in cache: %s" % (cached_file_folder,))

            url = '%s%s' % (HOST, self.path)
            print("url: %s" % (url,))
            response = requests.put(url, data=self.data_string, headers=self.headers, stream=True)

            if response.status_code == 200:
                self._write_cache(cached_file_folder, cached_file_header, cached_file_content, response)
            else:
                print('Error when requesting file :')
                print('Requested url : %s' % (url,))
                print('Status code : %s' % (response.status_code,))
                print('Content : %s' % (response.content,))
                self.send_response(response.status_code)
                self.end_headers()
                self.wfile.write('404 Not found, nothing found on the remote server !!!')
                return response.content
        else:
            print("Request for data present in cache: %s" % (cached_file_folder,))

        self._send_content(cached_file_header, cached_file_content)



# Main code that start the HTTP server
httpd = SocketServer.ForkingTCPServer(('', PORT), Proxy)
httpd.allow_reuse_address = True
print "Listening on port "+ str(PORT) + "(Press Ctrl+C/Ctrl+Z to stop HTTPD Caching script)"
print "Caching folder " + CACHE_ROOT + ", Tmp folder for generated files " + TMP_ROOT 
signal.signal(signal.SIGINT, signal_handler)
httpd.serve_forever()