#!/usr/bin/env python3
import sys
from os import environ
from os.path import exists
from os import rename

from elasticsearch.ElasticsearchConfig import ElasticsearchConfig
from elasticsearch.helpers import get_env, write_file

if __name__ == '__main__':
    conf_dir = environ.get( "CONF_DIR" ) if environ.get( "CONF_DIR" ) else "/opt/elasticsearch/config"

    if exists( f"{conf_dir}/elasticsearch.yml" ):
        print( "existing config detected, overwriting. the original will be preserved" )
        rename( f"{conf_dir}/elasticsearch.yml", f"{conf_dir}/elasticsearch.original.yml" )

    es_props = get_env( "ES_" )
    config = ElasticsearchConfig( es_props )

    if config.is_valid():
        es_yaml = updated = config.make_config()
        write_file( conf_dir, "elasticsearch.yml", es_yaml )
    else:
        print( "config is not valid, see previous errors for more info" )
        sys.exit( 1 )