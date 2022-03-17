#!/bin/bash

echo "Elasticsearch environment variables"
echo "ES_ROLE=$ES_ROLE"
echo "ES_NODE_NAME=$ES_NODE_NAME"
echo "ES_NODE_ID=$ES_NODE_ID"
echo "ES_CLUSTER=$ES_CLUSTER"
echo "ES_MASTER=$ES_MASTER"
echo "ES_DISCOVERY_HOST=$ES_DISCOVERY_HOST"

/opt/app/bin/utils/elasticsearch-env-configurator.py

$ES_HOME/bin/elasticsearch -d

TIME_START=$(date +%s)
if [ -z "$ES_TIMEOUT" ]
then
  ES_TIMEOUT=60
fi

function wait_for_healthy() {
  STARTED=false
  HTTP_PORT="$((9200 + $ES_NODE_ID))"
  while [ !${STARTED} ];
  do
    TIME_NOW=$(date +%s)
    if (( TIME_NOW > (TIME_START + ES_TIMEOUT) ))
    then
      echo "Elasticsearch timed out ($ES_TIMEOUT seconds)"
      return 1
    fi

    STATUS=$(curl --silent --output /dev/stderr --write-out "%{http_code}" http://localhost:$HTTP_PORT)
    if [ $STATUS == 200 ]; then
      STARTED=true
      return 0
    fi
  done
}

echo "Waiting for Elasticsearch to start up..."
wait_for_healthy || exit 1

echo "Precreating the CDR index"
curl -X PUT  http://localhost:9200/cdr_search -H "Content-Type: application/json" -d "@/opt/elasticsearch/config/cdr-mapping.json"

curl -X PUT http://localhost:9200/cdr_search/_settings -H "Content-Type: application/json" -d '
{
  "index.mapping.nested_objects.limit": 50000
}
'

echo "Precreating the Tenants index"
curl -X PUT  http://localhost:9200/tenants -H "Content-Type: application/json" -d "@/opt/elasticsearch/config/tenant-mapping.json"

tail -f $ES_HOME/logs/dart-es.log

