from os import environ

from yaml import load, dump


def read_yaml( file: str ) -> dict:
    return load( open( file ).read() )


def write_file( target_dir: str, filename: str, content: dict ):
    with open( f"{target_dir}/{filename}", "+w" ) as file:
        file.write( dump( content, default_flow_style = False ) )


def get_env( prefix: str ) -> dict:
    props = { }
    for key in environ.keys():
        if isinstance( key, str ) and key.startswith( prefix ):
            props[ key ] = environ.get( key )
    return props