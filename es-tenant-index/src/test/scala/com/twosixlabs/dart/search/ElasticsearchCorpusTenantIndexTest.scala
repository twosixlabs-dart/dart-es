package com.twosixlabs.dart.search

import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex.DocIdMissingFromIndexException
import com.twosixlabs.dart.auth.tenant.CorpusTenantIndexTest
import com.twosixlabs.dart.search.test.EsTestBase
import com.twosixlabs.dart.test.tags.annotations.{IntegrationTest, WipTest}
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

@WipTest
@IntegrationTest
class ElasticsearchCorpusTenantIndexTest
  extends CorpusTenantIndexTest( ElasticsearchCorpusTenantIndex( "localhost" ), writeDelayMs = 3000 )
    with EsTestBase
    with BeforeAndAfterEach {

    override implicit val ec : ExecutionContext = scala.concurrent.ExecutionContext.global

    override def beforeAll( ) : Unit = {
        super.beforeAll()

        import com.sksamuel.elastic4s.ElasticDsl._

        val prep = Future.sequence {
            ( docIds map { docId =>
                client.execute {
                    indexInto( CDR_INDEX ).id( docId ).doc(
                        s"""{"document_id":"$docId","tenants":[]}"""
                        )
                }
            } )
        }

        Await.result( prep, 10 seconds )
        Thread.sleep( 3000 )
    }

    override def beforeEach( ) : Unit = {
        Await.result( index.allTenants.flatMap( ts => index.removeTenants( ts.map( _.id ) ) ), 10 seconds )
        Thread.sleep( 3000 )
        super.beforeEach()
    }

    behavior of s"$className.addDocumentToTenant"

    it should "throw DocIdMissingFromIndexException if doc id is not in elasticsearch index" in {
        Await.result( index.addTenant( "test-tenant-for-bad-doc" ), 5.seconds )
        Thread.sleep( writeDelayMs )
        Try( Await.result( index.addDocumentToTenant( "fake-doc-id-zxzx", "test-tenant-for-bad-doc" ), 5.seconds ) ) match {
            case Success( _ ) => fail( "method succeeded (no exception thrown)" )
            case Failure( e : DocIdMissingFromIndexException ) => e.getMessage should include( "fake-doc-id-zxzx" )
            case Failure( e ) => fail( s"threw wrong exception: ${e.getMessage}" )
        }
        Await.result( index.removeTenant( "test-tenant-for-bad-doc" ), 5.seconds )
    }

}
