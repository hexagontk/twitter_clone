
#
# RUNTIME
#
FROM openjdk:11
LABEL description="minitwit API"
USER root

ENV TZ Europe/Madrid
ENV PROJECT main

# Machine setup
VOLUME /tmp
EXPOSE 9090

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Project install
COPY $PROJECT/build/install/$PROJECT /opt/$PROJECT
RUN touch /opt/$PROJECT

# Process execution
WORKDIR /opt/$PROJECT
ENTRYPOINT /opt/$PROJECT/bin/$PROJECT
