
#
# RUNTIME
#
FROM openjdk:17

USER root

ENV TZ UTC
ENV PROJECT twitter_clone

# Machine setup
VOLUME /tmp
EXPOSE 9090

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Project install
COPY build/install/$PROJECT /opt/$PROJECT
RUN touch /opt/$PROJECT

# Process execution
WORKDIR /opt/$PROJECT
ENTRYPOINT /opt/$PROJECT/bin/$PROJECT
