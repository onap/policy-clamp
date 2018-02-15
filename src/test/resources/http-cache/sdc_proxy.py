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
# ECOMP is a trademark and service mark of AT&T Intellectual Property.
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

parser = argparse.ArgumentParser(description="SDC Cache & Replay")
parser.add_argument("--username", "-u", type=str, help="Set the username for contacting SDC")
parser.add_argument("--password", "-p", type=str, help="Set the password for contacting SDC")
parser.add_argument("--root",     "-r", default=tempfile.mkdtemp, type=str, help="Root folder for the proxy cache")
parser.add_argument("--proxy"         , type=str, help="Url of the  Act as a proxy. If not set, this script only uses the cache and will return a 404 if files aren't found")
parser.add_argument("--port",     "-P", type=int, default="8081", help="Port on which the proxy should listen to")
parser.add_argument("--verbose",  "-v", type=bool, help="Print more information in case of error")
options = parser.parse_args()


PORT = options.port
SDC_HOST = options.proxy
SDC_AUTH = (options.username, options.password)
SDC_HEADERS = {'X-ECOMP-InstanceID':'CLAMP'}
CACHE_ROOT = options.root

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
            print("%s : %s" % (header, value))

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

        self.print_headers()
        self.check_credentials()

        cached_file = '%s/%s' % (CACHE_ROOT, self.path,)
        print("Cached file name before escaping : %s" % cached_file)
        cached_file = cached_file.replace('<','&#60;').replace('>','&#62;').replace('?','&#63;').replace('*','&#42;').replace('\\','&#42;').replace(':','&#58;').replace('|','&#124;')
        print("Cached file name after escaping (used for cache storage) : %s" % cached_file)
        cached_file_content = "%s/.file" % (cached_file,)
        cached_file_header = "%s/.header" % (cached_file,)

        _file_available = os.path.exists(cached_file_content)
        if not _file_available and not SDC_HOST:
            self.send_response(404)
            return "404 Not found"

        if not _file_available:
            print("SDC Request for data currently not present in cache: %s" % (cached_file,))
            url = '%s%s' % (SDC_HOST, self.path)
            response = requests.get(url, auth=SDC_AUTH, headers=SDC_HEADERS, stream=True)

            if response.status_code == 200:
                self._write_cache(cached_file, cached_file_header, cached_file_content, response)
            else:
                print('Error when requesting file :')
                print('Requested url : %s' % (url,))
                print('Status code : %s' % (response.status_code,))
                print('Content : %s' % (response.content,))
                self.send_response(response.status_code)
                return response.content

        self._send_content(cached_file_header, cached_file_content)

# Main code that start the HTTP server
httpd = SocketServer.ForkingTCPServer(('', PORT), Proxy)
httpd.allow_reuse_address = True
print "Listening on port "+ str(PORT) + " and caching in " + CACHE_ROOT + "(Press Ctrl+C to stop HTTPD Caching script)"
signal.signal(signal.SIGINT, signal_handler)
httpd.serve_forever()