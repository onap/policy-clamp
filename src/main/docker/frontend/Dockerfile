###
# ============LICENSE_START=======================================================
# ONAP CLAMP
# ================================================================================
# Copyright (C) 2019 AT&T Intellectual Property. All rights
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

# build environment
FROM node:12.10.0-alpine as build
WORKDIR /app
#ENV PATH /app/node_modules/.bin:$PATH
COPY onap-clamp-frontend/ /app/
RUN npm install --silent
RUN npm run build

FROM nginx:1.17.0-alpine

MAINTAINER "The Onap Team"
LABEL Description="This image contains Clamp frontend"

ARG http_proxy
ARG https_proxy
ENV HTTP_PROXY=$http_proxy
ENV HTTPS_PROXY=$https_proxy
ENV http_proxy=$HTTP_PROXY
ENV https_proxy=$HTTPS_PROXY

RUN addgroup onap && adduser -D -G onap clamp
RUN mkdir /var/log/onap
RUN chmod a+rwx /var/log/onap

COPY --from=build /app/build /usr/share/nginx/html
COPY --from=build /app/ssl /etc/ssl
RUN rm /etc/nginx/conf.d/default.conf
RUN ln -sf /dev/stdout /var/log/nginx/access.log && ln -sf /dev/stderr /var/log/nginx/error.log

COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

WORKDIR /app

RUN chown -R clamp:onap /app && chmod -R 755 /app && \
        chown -R clamp:onap /var/cache/nginx && \
        chown -R clamp:onap /var/log/nginx && \
        chown -R clamp:onap /etc/nginx/conf.d
RUN touch /var/run/nginx.pid && \
        chown -R clamp:onap /var/run/nginx.pid

USER clamp
EXPOSE 2443
CMD ["nginx", "-g", "daemon off;"]