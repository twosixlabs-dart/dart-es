package com.twosixlabs.dart.search

import com.twosixlabs.cdr4s.annotations.{FacetScore, OffsetTag}
import com.twosixlabs.cdr4s.core.{CdrAnnotation, FacetAnnotation, OffsetTagAnnotation}
import com.twosixlabs.dart.search.exceptions.CdrNotFoundException
import com.twosixlabs.dart.search.test.EsTestBase
import com.twosixlabs.dart.search.test.TestObjectMother.CDR_TEMPLATE
import com.twosixlabs.dart.test.tags.annotations.IntegrationTest
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt, SECONDS}
import scala.language.postfixOps

@IntegrationTest
class EsIndexTest extends EsTestBase with BeforeAndAfterEach {

    val testDocId : String = "634f221da953e225ff9669bd620415df"

    val searchIndex : ElasticsearchIndex = new ElasticsearchIndex( client )

    override def afterEach( ) {
        import com.sksamuel.elastic4s.ElasticDsl._

        client.execute( deleteById( "cdr_search", testDocId ) )
        client.execute( deleteById( "cdr_search", "cd8703d44edf93848e945e3e0c6cd4b7" ) )
        Thread.sleep( 1500 )
    }

    "Elasticsearch search indexer" should "be able to index and then update annotations for a real doc right in a row" in {
        val searchIndex : ElasticsearchIndex = new ElasticsearchIndex( client )

        val doc = CDR_TEMPLATE.copy( documentId = testDocId )

        val cdrAnnotation1 = FacetAnnotation( "test-label1", "0.0.1", List( FacetScore( "test-value-1", None ) ) )
        val cdrAnnotation2 = OffsetTagAnnotation( "test-label-2", "0.0.1", List( OffsetTag( 0, 5, Some( "hello" ), "Greeting", Some( 0.789320001 ) ) ) )
        val cdrAnnotation3 = OffsetTagAnnotation( "test-label-3", "0.0.1", List( OffsetTag( 0, 5, Some( "world" ), "Greetee", Some( 0.789320001 ) ) ) )

        Await.result( searchIndex.upsertDocument( doc ), 10 seconds)
        Thread.sleep( 5000 )
        Await.result( searchIndex.updateAnnotation( doc, cdrAnnotation1 ), 10 seconds )
        Thread.sleep( 5000 )
        Await.result( searchIndex.updateAnnotation( doc, cdrAnnotation2 ), Duration( 3, SECONDS ) )
        Await.result( searchIndex.updateAnnotation( doc, cdrAnnotation3 ), Duration( 3, SECONDS ) )

        Thread.sleep( 1000 )

        val upsertResult = Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) )
        upsertResult.annotations.size shouldBe 3
        upsertResult.annotations.count( _.getClass == classOf[ FacetAnnotation ] ) shouldBe 1
        upsertResult.annotations.count( _.getClass == classOf[ OffsetTagAnnotation ] ) shouldBe 2
        upsertResult.annotations.count( _.label == cdrAnnotation1.label ) shouldBe 1
        upsertResult.annotations.count( _.label == cdrAnnotation2.label ) shouldBe 1
        upsertResult.annotations.count( _.label == cdrAnnotation3.label ) shouldBe 1
        upsertResult.annotations should contain( cdrAnnotation1 )
        upsertResult.annotations should contain( cdrAnnotation2 )
        upsertResult.annotations should contain( cdrAnnotation3 )
    }

    "Elasticsearch search indexer" should "insert a document" in {
        val searchIndex : ElasticsearchIndex = new ElasticsearchIndex( client )

        val doc = CDR_TEMPLATE.copy( documentId = testDocId )

        // Ensure relevant doc is not yet in index
        an[ CdrNotFoundException ] should be thrownBy ( Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) ) )

        Await.result( searchIndex.upsertDocument( doc ), Duration( 3, SECONDS ) )

        Thread.sleep( 1500 )

        val insertResult = Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) )

        insertResult.documentId shouldBe doc.documentId
        insertResult.extractedText shouldBe doc.extractedText
        insertResult.extractedNtriples shouldBe doc.extractedNtriples
    }

    "Elasticsearch search indexer" should "add annotations to a document with no existing annotations" in {
        val searchIndex : ElasticsearchIndex = new ElasticsearchIndex( client )

        val doc = CDR_TEMPLATE.copy( documentId = testDocId )

        // Confirm doc has been deleted
        an[ CdrNotFoundException ] should be thrownBy ( Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) ) )

        // Ensure doc is in index
        Await.result( searchIndex.upsertDocument( doc ), Duration( 3, SECONDS ) )

        Thread.sleep( 1500 )

        val insertResult = Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) )

        insertResult.documentId shouldBe doc.documentId
        insertResult.extractedText shouldBe doc.extractedText
        insertResult.extractedNtriples shouldBe doc.extractedNtriples

        val annotation : CdrAnnotation[ _ ] = FacetAnnotation( "test-label", "1.0.0", List( FacetScore( "value-1", None ), FacetScore( "value-2", None ) ),
                                                               CdrAnnotation.DERIVED )
        Await.result( searchIndex.updateAnnotation( doc, annotation ), Duration( 3, SECONDS ) )

        Thread.sleep( 1500 )

        val upsertResult = Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) )
        upsertResult.annotations.size shouldBe 1
    }

    "Elasticsearch search indexer" should "add annotations to a document with no annotations field at all" in {
        val searchIndex : ElasticsearchIndex = new ElasticsearchIndex( client )

        val doc = CDR_TEMPLATE.copy( documentId = testDocId, annotations = null )

        // Confirm doc has been deleted
        an[ CdrNotFoundException ] should be thrownBy ( Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) ) )

        // Ensure doc is in index
        Await.result( searchIndex.upsertDocument( doc ), Duration( 3, SECONDS ) )

        Thread.sleep( 1500 )

        val insertResult = Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) )

        insertResult.documentId shouldBe doc.documentId
        insertResult.extractedText shouldBe doc.extractedText
        insertResult.extractedNtriples shouldBe doc.extractedNtriples

        val annotation : CdrAnnotation[ _ ] = FacetAnnotation( "test-label", "1.0.0", List( FacetScore( "value-1", None ), FacetScore( "value-2", None ) ),
                                                               CdrAnnotation.DERIVED )
        Await.result( searchIndex.updateAnnotation( doc, annotation ), Duration( 3, SECONDS ) )

        Thread.sleep( 1500 )

        val upsertResult = Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) )
        upsertResult.annotations.size shouldBe 1
    }

    "Elasticsearch search indexer" should "add annotations to a document with pre-existing annotations" in {
        val searchIndex : ElasticsearchIndex = new ElasticsearchIndex( client )

        val doc = CDR_TEMPLATE.copy( documentId = testDocId, annotations = List( FacetAnnotation( "factiva-existing", "1", List( FacetScore( "test", None ), FacetScore
        ( "test 2", Some( 1.0 ) ) ), CdrAnnotation.STATIC ) ) )
        Await.result( searchIndex.upsertDocument( doc ), Duration( 3, SECONDS ) )

        Thread.sleep( 1500 )

        val annotation : CdrAnnotation[ _ ] = OffsetTagAnnotation( "test-tags", "1.0", List( OffsetTag( 0, 1, Some( "test" ), "ORG", Some( 0.789320001 ) ), OffsetTag( 0, 1, Some( "test 2" ), "GPE", Some( 0.789320001 ) )
                                                                                             ) )
        Await.result( searchIndex.updateAnnotation( doc, annotation ), Duration( 3, SECONDS ) )

        Thread.sleep( 1500 )

        val upsertResult = Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) )
        upsertResult.annotations.count( _.getClass == classOf[ OffsetTagAnnotation ] ) shouldBe 1
        upsertResult.annotations.count( _.getClass == classOf[ FacetAnnotation ] ) shouldBe 1
        upsertResult.annotations.size shouldBe 2
    }

    "Elasticsearch search indexer" should "add replace annotation with the same label" in {
        val searchIndex : ElasticsearchIndex = new ElasticsearchIndex( client )

        val doc = CDR_TEMPLATE.copy( documentId = testDocId,
                                     annotations = List( FacetAnnotation( "factiva-existing", "1", List( FacetScore( "test", None ), FacetScore( "test 2", Some( 1.0 )
                                                                                                                                                           ) ), CdrAnnotation
                                       .STATIC ),
                                                         OffsetTagAnnotation( "factiva-existing-2", "1", List( OffsetTag( 0, 10, None, "tag-1", Some( 0.789320001 ) ), OffsetTag( 15, 25, None,
                                                                                                                                                             "tag-2", Some( 0.789320001 ) ) ),
                                                                              CdrAnnotation.DERIVED ),
                                                         OffsetTagAnnotation( "factiva-existing-3", "1", List( OffsetTag( 2, 8, None, "tag-a", Some( 0.789320001 ) ), OffsetTag( 17, 23, None,
                                                                                                                                                                                 "tag-b", Some( 0.789320001 )
                                                                                                                                                            ) ), CdrAnnotation
                                                           .STATIC ) ) )
        Await.result( searchIndex.upsertDocument( doc ), Duration( 3, SECONDS ) )

        Thread.sleep( 1500 )

        val annotation : CdrAnnotation[ _ ] = OffsetTagAnnotation( "factiva-existing-2", "2.0", List( OffsetTag( 0, 1, Some( "test" ), "ORG", Some( 0.789320001 ) ), OffsetTag( 0, 1, Some( "test 2" )
                                                                                                                                                           , "GPE", Some( 0.789320001 ) ) ) )
        Await.result( searchIndex.updateAnnotation( doc, annotation ), Duration( 3, SECONDS ) )

        Thread.sleep( 1500 )

        val upsertResult = Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) )
        upsertResult.annotations.size shouldBe 3
        upsertResult.annotations.count( _.getClass == classOf[ FacetAnnotation ] ) shouldBe 1
        upsertResult.annotations.count( _.getClass == classOf[ OffsetTagAnnotation ] ) shouldBe 2
        upsertResult.annotations.count( _.label == "factiva-existing" ) shouldBe 1
        upsertResult.annotations.count( _.label == "factiva-existing-2" ) shouldBe 1
        upsertResult.annotations.count( _.label == "factiva-existing-3" ) shouldBe 1
        upsertResult.annotations.find( _.label == "factiva-existing-2" ).get.asInstanceOf[ OffsetTagAnnotation ].content.exists( _.tag == "ORG" )
        upsertResult.annotations.find( _.label == "factiva-existing-2" ).get.asInstanceOf[ OffsetTagAnnotation ].version shouldBe "2.0"
    }

}