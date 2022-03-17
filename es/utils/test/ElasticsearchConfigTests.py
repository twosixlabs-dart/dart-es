import unittest

from os import environ
from os import mkdir
from os.path import exists

from shutil import rmtree
from elasticsearch.helpers import write_file, read_yaml
from elasticsearch.ElasticsearchConfig import ElasticsearchConfig


class HelperTests( unittest.TestCase ):
    environ[ "CONF_DIR" ] = "target"

    def setUp( self ):
        if exists( environ[ "CONF_DIR" ] ) is False:
            mkdir( environ[ "CONF_DIR" ] )

    def tearDown( self ):
        rmtree( environ.get( "CONF_DIR" ) )

    def test_valid_master( self ):
        env = {
            "ES_MASTER": "dart-es-master",
            "ES_ROLE": "master",
            "ES_NODE_NAME": "dart-es-master",
            "ES_NODE_ID": "0",
            "ES_CLUSTER": "dart-es"
        }
        config = ElasticsearchConfig( env )
        assert (config.is_valid())

    def test_valid_replica( self ):
        env = {
            "ES_MASTER": "dart-es-master",
            "ES_ROLE": "replica",
            "ES_NODE_NAME": "dart-es-replica-1",
            "ES_NODE_ID": "1",
            "ES_CLUSTER": "dart-es",
            "ES_DISCOVERY_HOST": "localhost"
        }
        config = ElasticsearchConfig( env )
        assert (config.is_valid())

    def test_write_master_file( self ):
        conf_dir = environ[ "CONF_DIR" ]
        env = {
            "ES_MASTER": "dart-es-master",
            "ES_ROLE": "master",
            "ES_NODE_NAME": "dart-es-master",
            "ES_NODE_ID": "0",
            "ES_CLUSTER": "dart-es"
        }

        es_config = ElasticsearchConfig( env )
        if es_config.is_valid():
            config = es_config.make_config()
            write_file( conf_dir, "elasticsearch.yml", config )
            actual_file = read_yaml( f"{conf_dir}/elasticsearch.yml" )
            expected_file = read_yaml( "test/resources/expected-master.yml" )
            print( actual_file )
            print( expected_file )
            assert (expected_file == actual_file)
        else:
            self.fail( "configuration was not valid" )

    def test_write_replica_file( self ):
        conf_dir = environ[ "CONF_DIR" ]
        env = {
            "ES_ROLE": "replica",
            "ES_NODE_ID": "1",
            "ES_NODE_NAME": "dart-es-replica-1",
            "ES_CLUSTER": "dart-es",
            "ES_MASTER": "dart-es-master",
            "ES_DISCOVERY_HOST": "localhost"
        }
        es_config = ElasticsearchConfig( env )

        if es_config.is_valid():
            config = es_config.make_config()
            write_file( conf_dir, "elasticsearch.yml", config )
            actual_file = read_yaml( f"{conf_dir}/elasticsearch.yml" )
            expected_file = read_yaml( "test/resources/expected-replica.yml" )
            print( actual_file )
            print( expected_file )
            assert (expected_file == actual_file)
        else:
            self.fail( "configuration was not valid" )


if __name__ == '__main__':
    unittest.main()