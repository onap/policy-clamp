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

RUN groupadd -r onap && useradd --no-log-init -r -g onap clamp
VOLUME /opt/clamp/config
RUN mkdir /var/log/onap
RUN chmod a+rwx /var/log/onap

COPY onap-clamp/clamp.jar /opt/clamp/app.jar
RUN chmod 700 /opt/clamp/app.jar

RUN chown -R clamp:onap /opt/clamp

USER clamp
WORKDIR /opt/clamp/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xms256m", "-Xmx1g", "-jar" ,"./app.jar"]
