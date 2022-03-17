#!/bin/bash

if [ ! -f distro/elasticsearch.tgz ]; then
    curl https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-oss-7.4.0-linux-x86_64.tar.gz --output es/distro/elasticsearch.tgz
fi