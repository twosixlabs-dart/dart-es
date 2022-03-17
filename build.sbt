import sbt.{Def, _}
import Dependencies._

/*
   ##############################################################################################
   ##                                                                                          ##
   ##                                  SETTINGS DEFINITIONS                                    ##
   ##                                                                                          ##
   ##############################################################################################
 */

// integrationConfig and wipConfig are used to define separate test configurations for integration testing
// and work-in-progress testing
lazy val IntegrationConfig = config( "integration" ) extend( Test )
lazy val WipConfig = config( "wip" ) extend( Test )

// Give services access to es mappings directory for testing
lazy val setTestResourcesDir = Test / unmanagedResourceDirectories += baseDirectory.value / ".." / "es" / "mappings"

lazy val commonSettings : Seq[ Def.Setting[ _ ] ] = {
    inConfig( IntegrationConfig )( Defaults.testTasks ) ++
    inConfig( WipConfig )( Defaults.testTasks ) ++
    Seq(
        organization := "com.twosixlabs.dart.elasticsearch",
        scalaVersion := "2.12.13",
        resolvers ++= Seq( "Maven Central" at "https://repo1.maven.org/maven2/",
                           "JCenter" at "https://jcenter.bintray.com",
                           "Local Ivy Repository" at s"file://${System.getProperty( "user.home" )}/.ivy2/local/default" ),
        libraryDependencies ++= Seq(
            dartCommons,
            cdr4s,
            logging,
            elastic4s,
            dartAuth,
            unitTesting,
            betterFiles,
        ).flatten,
        // `sbt test` should skip tests tagged IntegrationTest
        Test / testOptions := Seq( Tests.Argument( "-l", "com.twosixlabs.dart.test.tags.annotations.IntegrationTest" ) ),
        // `sbt integration:test` should run only tests tagged IntegrationTest
        IntegrationConfig / parallelExecution := false,
        concurrentRestrictions in Global += Tags.limitSum( 1, Tags.Test, Tags.Untagged ),
        IntegrationConfig / testOptions := Seq( Tests.Argument( "-n", "annotations.IntegrationTest" ) ),
        // `sbt wip:test` should run only tests tagged WipTest
        WipConfig / testOptions := Seq( Tests.Argument( "-n", "annotations.WipTest" ) ),
        setTestResourcesDir,
        publishTo := {
	    // TODO
	    None
        },
        publishMavenStyle := true,
   )
}

/*
   ##############################################################################################
   ##                                                                                          ##
   ##                                  PROJECT DEFINITIONS                                     ##
   ##                                                                                          ##
   ##############################################################################################
 */

lazy val root = ( project in file( "." ) )
  .aggregate( esUtil, searchIndex, tenantIndex )
  .settings(
      name := "dart-es",
      publish := {},
   )

lazy val esUtil = ( project in file( "es-util" ) )
  .configs( WipConfig, IntegrationConfig )
  .settings(
      commonSettings,
  )

lazy val utilDependency = esUtil % "test->test;compile->compile;test->compile"

lazy val searchIndex = ( project in file( "es-search-index" ) )
  .dependsOn( utilDependency )
  .configs( WipConfig, IntegrationConfig )
  .settings(
      commonSettings,
  )

lazy val tenantIndex = ( project in file( "es-tenant-index" ) )
  .dependsOn( utilDependency )
  .configs( WipConfig, IntegrationConfig )
  .settings(
      commonSettings,
   )

//ThisBuild / useCoursier := false

