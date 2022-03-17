#!/bin/bash
echo "ES_NODE_NAME=$ES_NODE_NAME"

HTTP_PORT="$((9200 + $ES_NODE_ID))"
    
STATUS=$(curl --silent --output /dev/stderr --write-out "%{http_code}" http://localhost:$HTTP_PORT)
if [ $STATUS == 200 ]; then
  echo "elasticsearch is up"
  exit 0
else
  echo "elasticsearch is not up"
  exit 1
fi
