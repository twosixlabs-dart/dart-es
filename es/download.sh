#!/bin/bash

if [ ! -f ./es/distro/elasticsearch.tgz ]; then
    mkdir -p ./es/distro/
    curl https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-oss-7.4.0-linux-x86_64.tar.gz --output ./es/distro/elasticsearch.tgz
fi