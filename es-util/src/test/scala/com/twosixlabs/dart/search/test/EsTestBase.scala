package com.twosixlabs.dart.search.test

import better.files.Resource
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import com.sksamuel.elastic4s.http.JavaClient
import com.twosixlabs.cdr4s.core.CdrDocument
import com.twosixlabs.dart.exceptions.ExceptionImplicits.FutureExceptionLogging
import com.twosixlabs.dart.search.test.TestObjectMother.CDR_TEMPLATE
import com.twosixlabs.dart.test.TestUtils
import com.twosixlabs.dart.test.base.StandardTestBase3x
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}

trait EsTestBase extends StandardTestBase3x with BeforeAndAfterAll {

    val TEST_HOST : String = "localhost"
    val TEST_PORT : Int = 9200

    implicit val ec : ExecutionContext = scala.concurrent.ExecutionContext.global

    val writeTimeout : Duration = 1.second

    val client : ElasticClient = ElasticClient( JavaClient( ElasticProperties( s"http://${TEST_HOST}:${TEST_PORT}" ) ) )

    val cdrTemplate : CdrDocument = CDR_TEMPLATE

    val CDR_INDEX : String = "cdr_search"
    val TENANT_INDEX : String = "tenants"

    def deleteIndices( ) : Future[ Unit ] = {
        import com.sksamuel.elastic4s.ElasticDsl._

        for {
            _ <- client.execute {
                    deleteIndex( TENANT_INDEX )
                }  recover { case _ => () }
            _ <- client.execute {
                    deleteIndex( CDR_INDEX )
                } recover { case _ => () }
        } yield ()

    }

    def createIndices( ) : Future[ Unit ] = {
        import com.sksamuel.elastic4s.ElasticDsl._

        for {
            _ <- ( client.execute {
                createIndex( TENANT_INDEX ).source( Resource.getAsString( "tenant-mapping.json" ) )
            } logged ) recover { case _ => () }
            _ <- ( client.execute {
                createIndex( CDR_INDEX ).source( Resource.getAsString( "cdr-mapping.json" ) )
            } logged ) recover { case _ => () }
        } yield ()
    }

    def awaitWrite[ T ]( fut : Future[ T ] ) : T = {
        val res = Await.result( fut, 10 seconds )
        Thread.sleep( 5000 )
        res
    }

    override def beforeAll( ) : Unit = {
        import com.sksamuel.elastic4s.ElasticDsl._

        awaitWrite( deleteIndices() recover { case _ => () } )
        awaitWrite( createIndices() recover { case _ => () } )

        require( Await.result( client.execute( indexExists( TENANT_INDEX ) ).map( _.result.exists ), 5 seconds ) )
        require( Await.result( client.execute( indexExists( CDR_INDEX ) ).map( _.result.exists ), 5 seconds ) )

        super.beforeAll()
    }

    override def afterAll( ) : Unit = {
        super.afterAll()
        awaitWrite( deleteIndices() )
    }

}
