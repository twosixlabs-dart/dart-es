package com.twosixlabs.dart.search

import annotations.IntegrationTest
import com.twosixlabs.cdr4s.annotations.{FacetScore, OffsetTag}
import com.twosixlabs.cdr4s.core.{CdrAnnotation, FacetAnnotation, OffsetTagAnnotation}
import com.twosixlabs.dart.search.test.EsTestBase
import com.twosixlabs.dart.search.test.TestObjectMother.CDR_TEMPLATE

import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit.SECONDS
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

@IntegrationTest
class ElasticsearchIndexTestSuite extends EsTestBase {

    "Elasticsearch search indexer" should "insert a document" in {
        val searchIndex : ElasticsearchIndex = new ElasticsearchIndex( client )

        val doc = CDR_TEMPLATE.copy( documentId = Random.alphanumeric.take( 16 ).mkString )
        Await.result( searchIndex.upsertDocument( doc ), Duration( 3, SECONDS ) )

        sleep( 1500 )

        val insertResult = Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) )

        insertResult.documentId shouldBe doc.documentId
        insertResult.extractedText shouldBe doc.extractedText
        insertResult.extractedNtriples shouldBe doc.extractedNtriples
    }

    "Elasticsearch search indexer" should "add annotations to a document with no existing annotations" in {
        val searchIndex : ElasticsearchIndex = new ElasticsearchIndex( client )

        val doc = CDR_TEMPLATE.copy( documentId = Random.alphanumeric.take( 16 ).mkString )
        Await.result( searchIndex.upsertDocument( doc ), Duration( 3, SECONDS ) )

        sleep( 1500 )

        val annotation : CdrAnnotation[ _ ] = OffsetTagAnnotation( "test-text", "1.0", List( OffsetTag( 1, 2, Some( "test-value" ), "test-tag", Some( 0.789320001 ) ) ) )
        Await.result( searchIndex.updateAnnotation( doc, annotation ), Duration( 3, SECONDS ) )

        sleep( 1500 )

        val upsertResult = Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) )
        upsertResult.annotations.size shouldBe 1
    }

    "Elasticsearch search indexer" should "add annotations to a document with pre-existing annotations" in {
        val searchIndex : ElasticsearchIndex = new ElasticsearchIndex( client )

        val doc = CDR_TEMPLATE.copy( documentId = Random.alphanumeric.take( 16 ).mkString, annotations = List( FacetAnnotation( "factiva-existing", "1", List( FacetScore( "test", None ), FacetScore( "test 2", Some( 1.0 ) ) ), CdrAnnotation.STATIC ) ) )
        Await.result( searchIndex.upsertDocument( doc ), Duration( 3, SECONDS ) )

        sleep( 1500 )

        val annotation : CdrAnnotation[ _ ] = OffsetTagAnnotation( "test-tags", "1.0", List( OffsetTag( 0, 1, Some( "test" ), "ORG", Some( 0.789320001 ) ), OffsetTag( 0, 1, Some( "test 2" ), "GPE", Some( 0.789320001 ) ) ) )
        Await.result( searchIndex.updateAnnotation( doc, annotation ), Duration( 3, SECONDS ) )

        sleep( 1500 )

        val upsertResult = Await.result( searchIndex.getDocument( doc.documentId ), Duration( 3, SECONDS ) )
        upsertResult.annotations.count( _.getClass == classOf[ OffsetTagAnnotation ] ) shouldBe 1
        upsertResult.annotations.count( _.getClass == classOf[ FacetAnnotation ] ) shouldBe 1
    }

}
