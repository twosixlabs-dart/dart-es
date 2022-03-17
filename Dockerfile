FROM ???

EXPOSE 9200
EXPOSE 9300

ENV ES_VERSION 7.4.0
ENV ES_HOME /opt/elasticsearch
ENV ES_OSS elasticsearch-${ES_VERSION}
ENV ES_JAVA_OPTS="$ES_JAVA_OPTS -Xmx2g -Xms2g"


# Default environment variables so that the image will run on it's own, this is
# intended to be overidden
ENV ES_ROLE master
ENV ES_NODE_NAME dart-es-master
ENV ES_MASTER dart-es-master
ENV ES_NODE_ID 0
ENV ES_CLUSTER dart-es

RUN groupadd elasticsearch && \
    useradd -g elasticsearch elasticsearch && \
    usermod -a -G root elasticsearch

# copy hbase and standalone config
ADD es/distro/elasticsearch.tgz /tmp

RUN mv /tmp/$ES_OSS /tmp/elasticsearch && \
    mv /tmp/elasticsearch /opt

COPY es/utils /opt/app/bin/utils
ENV PYTHONPATH "${PYTHONPATH}:/opt/app/bin/utils"
RUN pip install -r /opt/app/bin/utils/requirements.txt

ADD es/bin/* $ES_HOME/bin/
ADD es/mappings/* $ES_HOME/config/

RUN chown -R elasticsearch:root /opt && \
    chmod -R u+x /opt/elasticsearch/bin && \
    chmod -R u+x /opt/app/bin

USER elasticsearch

# ENTRYPOINT /bin/bash
ENTRYPOINT $ES_HOME/bin/start-es.sh
