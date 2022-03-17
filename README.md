# DART-Elasticsearch

DART Elasticsearch image build and service implementations.  
[![build and publish](https://github.com/twosixlabs-dart/dart-es/actions/workflows/build-and-publish.yml/badge.svg)](https://github.com/twosixlabs-dart/dart-es/actions/workflows/build-and-publish.yml)

## Image

To build the DART Elasticsearch image, run the following command in the project root:

```bash
make docker-build
```

To build the image and publish it, run:

```bash
make docker-push
```

This will pull the latest Elasticsearch distro, so be aware of potential breaking changes
with new builds.

## Dependencies
In addition to the handful of publicly available libraries it uses, dart-es has dependencies on a
number of other Scala libraries. In order to build DART these dependencies must be accessible
via the local filesystem (in the SBT cache) or over the network via
[Sonatype Nexus](https://www.sonatype.com/products/repository-oss-download) where they are
published. cdr4s requires the following dependencies to be built/installed:

| Group ID             | Artifact ID            |
|----------------------|------------------------|
| com.twosixlabs.dart  | dart-json_2.12         |
| com.twosixlabs.dart  | dart-test-base_2.12    |
| com.twosixlabs.dart  | dart-auth-commons_2.12 |
| com.twosixlabs.cdr4s | cdr4s-core_2.12        |
| com.twosixlabs.cdr4s | cdr4s-ladle-json_2.12  |
| com.twosixlabs.cdr4s | cdr4s-dart-json_2.12   |

## Building
This project is built using SBT. For more information on installation and configuration
of SBT please [see the documentation](https://www.scala-sbt.org/1.x/docs/)

The dart-es sbt projects consists of library modules containing no runnable main classes. 
The only supported build tasks are compilation, testing, and publication:

```bash
sbt clean         # clear out all build artifacts
sbt compile       
sbt test          # run all test suites
sbt publish       # publish all modules to maven
sbt publishLocal  # publish all modules locally
```

all tasks can be executed relative to a single module by prefixing the task with the
module name as defined in `build.sbt`:

```bash
sbt esUtil/compile
sbt tenantIndex/test
sbt searchIndex/publishLocal
```

## Project structure

The DART Elasticsearch image is defined in the `es` directory

The sbt build is into four subprojects:

1. es-search-index: interface and Elasticsearch implementation of basic search index read/write service
2. es-tenant-index: implementation of tenant-index in Elasticsearch
3. ladle-json: CDR DTOs and serialization/deserialization utilities used by the legacy extraction engine (ladle)
4. es-util: Utilities related to Elasticsearch (currently contains only a test base)
