class ElasticsearchConfig:
    def __init__( self, env: dict ):
        self.role = env.get( "ES_ROLE" )
        self.master_name = env.get( "ES_MASTER" )
        self.cluster_name = env.get( "ES_CLUSTER" )
        self.node_name = env.get( "ES_NODE_NAME" )
        self.node_id = int( env.get( "ES_NODE_ID" ) )
        self.discovery_host = env.get( "ES_DISCOVERY_HOST" )
        self.base_props = { "network.host": "0.0.0.0", "path.data": "/opt/app/data" }

    def make_config( self ) -> dict:
        self.base_props.update( self.as_es_props() )
        return self.base_props

    def as_es_props( self ) -> dict:
        print( self.role == "replica" )
        print( self.role )
        props = dict()
        props[ "cluster.name" ] = self.cluster_name
        props[ "cluster.initial_master_nodes" ] = [ self.master_name ]
        props[ "node.name" ] = self.node_name
        if self.role == "replica":
            props[ "discovery.zen.ping.unicast.hosts" ] = self.discovery_host
        props[ "http.port" ] = 9200 + self.node_id
        return props

    def is_valid( self ):
        if self.role is None or self.role not in [ "master", "replica" ]:
            print( "role is not set, a value of either 'master' or 'replica'" )
            return False
        if self.master_name is None:
            print( "no master name provided" )
            return False
        if self.cluster_name is None:
            print( "no cluster name provided" )
            return False
        if self.node_name is None:
            print( "no node name provided" )
            return False

        message_prefix = f"role is {self.role} and "
        if self.role == "master":
            if self.node_id is None or int( self.node_id ) != 0:
                print( f"{message_prefix} node id is not 0" )
                return False
            if self.discovery_host is not None:
                print( f"{message_prefix} and discovery host should not be set" )
        elif self.role == "replica":
            if self.node_id is None or int( self.node_id ) <= 0:
                print( f"{message_prefix} node id is not > 0" )
                return False
            if self.discovery_host is None:
                print( f"{message_prefix} and discovery host is not set" )
                return False
        return True

    def __str__( self ):
        return str( self.__dict__ )