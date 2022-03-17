#!/bin/bash

CONTAINER=elasticsearch

if [ -z "$1" ]; then
    echo "Missing parameter: CDR directory name"
    exit 1
fi

CDR_DIR=$1

export TMP_IMG=dart-es-tmp

docker build -t $TMP_IMG ../
docker-compose -f docker-compose-build-test-image.yml up -d || exit 1

function wait_for_healthy() {
    i=1
    while [[ $i -ne 0 ]] ;
    do
        curl localhost:9200/_cluster/health
        i=$?
    done
}

wait_for_healthy

./load_data.py "$CDR_DIR"

sleep 120

docker commit $CONTAINER docker.causeex.com/dart/dart-es/integration-test:sams-25k

docker-compose -f docker-compose-build-test-image.yml down

docker image rm $TMP_IMG
