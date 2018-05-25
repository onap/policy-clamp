FROM ubuntu:16.04

MAINTAINER "The Onap Team"
LABEL Description="This immage contains an updated ubuntu 16.04 with the openjdk installed" Version="16.04-8"

ARG http_proxy
ARG https_proxy
ENV HTTP_PROXY=$http_proxy
ENV HTTPS_PROXY=$https_proxy
ENV http_proxy=$HTTP_PROXY
ENV https_proxy=$HTTPS_PROXY

RUN test -n "$http_proxy" && echo "Acquire::Proxy \"http://$http_proxy\";" > /etc/apt/apt.conf.d/02proxy || true &&  \
    apt-get update &&  \
    apt-get -y dist-upgrade &&  \
    apt-get install -y openjdk-8-jre-headless

COPY onap-clamp/clamp.jar /opt/clamp/app.jar
VOLUME /etc
COPY onap-clamp/startService.sh /opt/clamp/startService.sh
RUN chmod 700 /opt/clamp/startService.sh

WORKDIR /opt/clamp/
ENTRYPOINT ./startService.sh 
