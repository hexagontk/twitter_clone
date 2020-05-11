#!/bin/sh

#
# Refer to `.git/hooks/pre-push.sample` for more information about this hook script.
# To clean Docker artifacts execute: `sudo docker system prune -af`
#

set -e

docker-compose up -d mongodb
./gradlew clean all
./gradlew test
docker-compose up -d --build --force-recreate
