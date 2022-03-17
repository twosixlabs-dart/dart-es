#! /usr/bin/env python3
# -*- coding: utf-8 -*-

from setuptools import setup, find_packages

with open('README.rst') as f:
    readme = f.read()

with open('LICENSE') as f:
    license = f.read()

setup(
    name = 'Elasticsearch configurator',
    version = '0.1.0',
    description = 'Configuration for Elasticsearch',
    long_description = readme,
    author = 'Michael Reynolds',
    author_email = 'me@kennethreitz.com',
    url = 'https://git.causeex.com/dart/dart-es',
    license = license,
    packages = find_packages( exclude = ('test', 'docs') )
)