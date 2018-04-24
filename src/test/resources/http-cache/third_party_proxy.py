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
parser.add_argument("--root",     "-r", default=tempfile.mkdtemp, type=str, help="Root folder for the proxy cache")
parser.add_argument("--proxy"         , type=str, help="Url of the  Act as a proxy. If not set, this script only uses the cache and will return a 404 if files aren't found")
parser.add_argument("--port",     "-P", type=int, default="8081", help="Port on which the proxy should listen to")
parser.add_argument("--verbose",  "-v", type=bool, help="Print more information in case of error")
parser.add_argument("--proxyaddress","-a", type=str, help="Address of this proxy, generally either third_party_proxy:8085 or localhost:8085 depending if started with docker-compose or not")
options = parser.parse_args()


PORT = options.port
HOST = options.proxy
AUTH = (options.username, options.password)
HEADERS = {'X-ECOMP-InstanceID':'CLAMP'}
CACHE_ROOT = options.root
PROXY_ADDRESS=options.proxyaddress

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

    def _write_cache(self,cached_file, header_file, content_file, response):
        os.makedirs(cached_file, True)
        with open(content_file, 'w') as f:
            f.write(response.raw.read())
        with open(header_file, 'w') as f:
            json.dump(dict(response.raw.headers), f)
    # Entry point of the code
    def do_GET(self):
        print("\n\n\nGot a GET request for %s " % self.path)

        self.print_headers()
        self.check_credentials()

        cached_file = '%s/%s' % (CACHE_ROOT, self.path,)
        print("Cached file name before escaping : %s" % cached_file)
        cached_file = cached_file.replace('<','&#60;').replace('>','&#62;').replace('?','&#63;').replace('*','&#42;').replace('\\','&#42;').replace(':','&#58;').replace('|','&#124;')
        print("Cached file name after escaping (used for cache storage) : %s" % cached_file)
        cached_file_content = "%s/.file" % (cached_file,)
        cached_file_header = "%s/.header" % (cached_file,)

        _file_available = os.path.exists(cached_file_content)

        if not _file_available:
            print("Request for data currently not present in cache: %s" % (cached_file,))

            if self.path.startswith("/dcae-service-types?asdcResourceId="):
                print "self.path start with /dcae-service-types?asdcResourceId=, generating response json..."
                uuidGenerated = str(uuid.uuid4())
                typeId = "typeId-" + uuidGenerated
                typeName = "typeName-" + uuidGenerated
                print "typeId generated: " + typeName + " and typeName: "+ typeId
                jsonGenerated = "{\"totalCount\":1, \"items\":[{\"typeId\":\"" + typeId + "\", \"typeName\":\"" + typeName +"\"}]}"
                print "jsonGenerated: " + jsonGenerated

                os.makedirs(cached_file, True)
                with open(cached_file_header, 'w') as f:
                    f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
                with open(cached_file_content, 'w') as f:
                    f.write(jsonGenerated)
            elif self.path.startswith("/dcae-operationstatus"):
                print "self.path start with /dcae-operationstatus, generating response json..."
                jsonGenerated =  "{\"operationType\": \"operationType1\", \"status\": \"succeeded\"}"
                print "jsonGenerated: " + jsonGenerated

                os.makedirs(cached_file, True)
                with open(cached_file_header, 'w') as f:
                    f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
                with open(cached_file_content, 'w') as f:
                    f.write(jsonGenerated)
            else:
                if not HOST:
                    self.send_response(404)
                    return "404 Not found"

                url = '%s%s' % (HOST, self.path)
                response = requests.get(url, auth=AUTH, headers=HEADERS, stream=True)

                if response.status_code == 200:
                    self._write_cache(cached_file, cached_file_header, cached_file_content, response)
                else:
                    print('Error when requesting file :')
                    print('Requested url : %s' % (url,))
                    print('Status code : %s' % (response.status_code,))
                    print('Content : %s' % (response.content,))
                    self.send_response(response.status_code)
                    return response.content
        else:
            print("Request for data currently present in cache: %s" % (cached_file,))

        self._send_content(cached_file_header, cached_file_content)

        if self.path.startswith("/dcae-service-types?asdcResourceId="):
            print "DCAE case deleting folder created " + cached_file
            shutil.rmtree(cached_file, ignore_errors=False, onerror=None)
        else:
            print "NOT in DCAE case deleting folder created " + cached_file

    def do_POST(self):
        print("\n\n\nGot a POST for %s" % self.path)
        self.check_credentials()
        self.data_string = self.rfile.read(int(self.headers['Content-Length']))
        print("data-string:\n %s" % self.data_string)
        print("self.headers:\n %s" % self.headers)

        cached_file = '%s/%s' % (CACHE_ROOT, self.path,)
        print("Cached file name before escaping : %s" % cached_file)
        cached_file = cached_file.replace('<','&#60;').replace('>','&#62;').replace('?','&#63;').replace('*','&#42;').replace('\\','&#42;').replace(':','&#58;').replace('|','&#124;')
        print("Cached file name after escaping (used for cache storage) : %s" % cached_file)
        cached_file_content = "%s/.file" % (cached_file,)
        cached_file_header = "%s/.header" % (cached_file,)

        _file_available = os.path.exists(cached_file_content)

        if not _file_available:
            if self.path.startswith("/sdc/v1/catalog/services/"):
                print "self.path start with /sdc/v1/catalog/services/, generating response json..."
                jsondata = json.loads(self.data_string)
                jsonGenerated = "{\"artifactName\":\"" + jsondata['artifactName'] + "\",\"artifactType\":\"" + jsondata['artifactType'] + "\",\"artifactURL\":\"" + self.path + "\",\"artifactDescription\":\"" + jsondata['description'] + "\",\"artifactChecksum\":\"ZjJlMjVmMWE2M2M1OTM2MDZlODlmNTVmZmYzNjViYzM=\",\"artifactUUID\":\"" + str(uuid.uuid4()) + "\",\"artifactVersion\":\"1\"}"
                print "jsonGenerated: " + jsonGenerated

                os.makedirs(cached_file, True)
                with open(cached_file_header, 'w') as f:
                    f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
                with open(cached_file_content, 'w') as f:
                    f.write(jsonGenerated)
            else:
                if not HOST:
                    self.send_response(404)
                    return "404 Not found"

                print("Request for data currently not present in cache: %s" % (cached_file,))

                url = '%s%s' % (HOST, self.path)
                print("url: %s" % (url,))
                response = requests.post(url, data=self.data_string, headers=self.headers, stream=True)

                if response.status_code == 200:
                    self._write_cache(cached_file, cached_file_header, cached_file_content, response)
                else:
                    print('Error when requesting file :')
                    print('Requested url : %s' % (url,))
                    print('Status code : %s' % (response.status_code,))
                    print('Content : %s' % (response.content,))
                    self.send_response(response.status_code)
                    return response.content
        else:
            print("Request for data present in cache: %s" % (cached_file,))

        self._send_content(cached_file_header, cached_file_content)

    def do_PUT(self):
        print("\n\n\nGot a PUT for %s " % self.path)
        self.check_credentials()
        self.data_string = self.rfile.read(int(self.headers['Content-Length']))
        print("data-string:\n %s" % self.data_string)
        print("self.headers:\n %s" % self.headers)

        cached_file = '%s/%s' % (CACHE_ROOT, self.path,)
        print("Cached file name before escaping : %s" % cached_file)
        cached_file = cached_file.replace('<','&#60;').replace('>','&#62;').replace('?','&#63;').replace('*','&#42;').replace('\\','&#42;').replace(':','&#58;').replace('|','&#124;')
        print("Cached file name after escaping (used for cache storage) : %s" % cached_file)
        cached_file_content = "%s/.file" % (cached_file,)
        cached_file_header = "%s/.header" % (cached_file,)

        _file_available = os.path.exists(cached_file_content)

        if not _file_available:
            if self.path.startswith("/dcae-deployments/"):
                print "self.path start with /dcae-deployments/, generating response json..."
                #jsondata = json.loads(self.data_string)
                jsonGenerated = "{\"links\":{\"status\":\"http:\/\/" + PROXY_ADDRESS + "\/dcae-operationstatus\",\"test2\":\"test2\"}}"
                print "jsonGenerated: " + jsonGenerated

                os.makedirs(cached_file, True)
                with open(cached_file_header, 'w') as f:
                    f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
                with open(cached_file_content, 'w') as f:
                    f.write(jsonGenerated)
            else:
                if not HOST:
                    self.send_response(404)
                    return "404 Not found"

                print("Request for data currently not present in cache: %s" % (cached_file,))

                url = '%s%s' % (HOST, self.path)
                print("url: %s" % (url,))
                response = requests.put(url, data=self.data_string, headers=self.headers, stream=True)

                if response.status_code == 200:
                    self._write_cache(cached_file, cached_file_header, cached_file_content, response)
                else:
                    print('Error when requesting file :')
                    print('Requested url : %s' % (url,))
                    print('Status code : %s' % (response.status_code,))
                    print('Content : %s' % (response.content,))
                    self.send_response(response.status_code)
                    return response.content
        else:
            print("Request for data present in cache: %s" % (cached_file,))

        self._send_content(cached_file_header, cached_file_content)


    def do_DELETE(self):
        print("\n\n\nGot a DELETE for %s " % self.path)
        self.check_credentials()
        print("self.headers:\n %s" % self.headers)

        cached_file = '%s/%s' % (CACHE_ROOT, self.path,)
        print("Cached file name before escaping : %s" % cached_file)
        cached_file = cached_file.replace('<','&#60;').replace('>','&#62;').replace('?','&#63;').replace('*','&#42;').replace('\\','&#42;').replace(':','&#58;').replace('|','&#124;')
        print("Cached file name after escaping (used for cache storage) : %s" % cached_file)
        cached_file_content = "%s/.file" % (cached_file,)
        cached_file_header = "%s/.header" % (cached_file,)

        _file_available = os.path.exists(cached_file_content)

        if not _file_available:
            if self.path.startswith("/dcae-deployments/"):
                print "self.path start with /dcae-deployments/, generating response json..."
                #jsondata = json.loads(self.data_string)
                jsonGenerated = "{\"links\":{\"status\":\"http:\/\/" + PROXY_ADDRESS + "\/dcae-operationstatus\",\"test2\":\"test2\"}}"
                print "jsonGenerated: " + jsonGenerated

                os.makedirs(cached_file, True)
                with open(cached_file_header, 'w') as f:
                    f.write("{\"Content-Length\": \"" + str(len(jsonGenerated)) + "\", \"Content-Type\": \"application/json\"}")
                with open(cached_file_content, 'w') as f:
                    f.write(jsonGenerated)
            else:
                if not HOST:
                    self.send_response(404)
                    return "404 Not found"

                print("Request for data currently not present in cache: %s" % (cached_file,))

                url = '%s%s' % (HOST, self.path)
                print("url: %s" % (url,))
                response = requests.put(url, data=self.data_string, headers=self.headers, stream=True)

                if response.status_code == 200:
                    self._write_cache(cached_file, cached_file_header, cached_file_content, response)
                else:
                    print('Error when requesting file :')
                    print('Requested url : %s' % (url,))
                    print('Status code : %s' % (response.status_code,))
                    print('Content : %s' % (response.content,))
                    self.send_response(response.status_code)
                    return response.content
        else:
            print("Request for data present in cache: %s" % (cached_file,))

        self._send_content(cached_file_header, cached_file_content)



# Main code that start the HTTP server
httpd = SocketServer.ForkingTCPServer(('', PORT), Proxy)
httpd.allow_reuse_address = True
print "Listening on port "+ str(PORT) + " and caching in " + CACHE_ROOT + "(Press Ctrl+C to stop HTTPD Caching script)"
signal.signal(signal.SIGINT, signal_handler)
httpd.serve_forever()