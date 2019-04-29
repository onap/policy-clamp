FROM openjdk:8u191-jdk-alpine3.9

MAINTAINER "The Onap Team"
LABEL Description="This immage contains alpine, openjdk 11 and clamp"

ARG http_proxy
ARG https_proxy
ENV HTTP_PROXY=$http_proxy
ENV HTTPS_PROXY=$https_proxy
ENV http_proxy=$HTTP_PROXY
ENV https_proxy=$HTTPS_PROXY

RUN addgroup onap && adduser -D -G onap clamp
VOLUME /opt/clamp/config
RUN mkdir /var/log/onap
RUN chmod a+rwx /var/log/onap

COPY onap-clamp/clamp.jar /opt/clamp/app.jar
RUN chmod 700 /opt/clamp/app.jar

RUN chown -R clamp:onap /opt/clamp

RUN apk add fontconfig
RUN apk add ttf-dejavu

RUN ln -s /usr/lib/libfontconfig.so.1 /usr/lib/libfontconfig.so && \
  ln -s /lib/libuuid.so.1 /usr/lib/libuuid.so.1 && \
  ln -s /lib/libc.musl-x86_64.so.1 /usr/lib/libc.musl-x86_64.so.1

ENV LD_LIBRARY_PATH /usr/lib

USER clamp
WORKDIR /opt/clamp/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xms256m", "-Xmx1g", "-jar" ,"./app.jar"]
