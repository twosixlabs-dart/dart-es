import sbt._

object Dependencies {

    val slf4jVersion = "1.7.20"
    val logbackVersion = "1.2.3"

    val betterFilesVersion = "3.8.0"

    val cdr4sVersion = "3.0.232"
    val dartCommonsVersion = "3.0.278"
    val dartAuthVersion = "3.1.159"

    val scalaTestVersion = "3.1.4"
    val scalaMockVersion = "4.2.0"

    val elastic4sVersion = "7.13.0"

    val mockitoVersion = "1.16.0"


    val cdr4s = Seq( "com.twosixlabs.cdr4s" %% "cdr4s-core" % cdr4sVersion,
                     "com.twosixlabs.cdr4s" %% "cdr4s-ladle-json" % cdr4sVersion,
                     "com.twosixlabs.cdr4s" %% "cdr4s-dart-json" % cdr4sVersion )

    val dartCommons = Seq( "com.twosixlabs.dart" %% "dart-json" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-test-base" % dartCommonsVersion % Test )

    val dartAuth = Seq( "com.twosixlabs.dart.auth" %% "core" % dartAuthVersion % "compile->compile;test->test" )

    val elastic4s = Seq( "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,
                         "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % elastic4sVersion,
                         "com.sksamuel.elastic4s" %% "elastic4s-json-jackson" % elastic4sVersion )

    val logging = Seq( "org.slf4j" % "slf4j-api" % slf4jVersion,
                       "ch.qos.logback" % "logback-classic" % logbackVersion )

    val betterFiles = Seq( "com.github.pathikrit" %% "better-files" % betterFilesVersion )

    val unitTesting = Seq( "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
                           "org.mockito" %% "mockito-scala-scalatest" % mockitoVersion % Test )
}
