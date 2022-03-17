package com.twosixlabs.dart.search

import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, ElasticRequest, HttpEntity, HttpResponse, Indexable, Response}
import com.twosixlabs.cdr4s.core.{CdrAnnotation, CdrDocument}
import com.twosixlabs.cdr4s.json.dart.DartJsonFormat
import com.twosixlabs.dart.auth.tenant.CorpusTenant
import com.twosixlabs.dart.json.JsonFormat
import com.twosixlabs.dart.json.JsonFormat.{marshalFrom, unmarshalTo}
import com.twosixlabs.dart.search.ElasticsearchIndex.DART_JSON
import com.twosixlabs.dart.search.exceptions.{CdrNotFoundException, CdrUnreadableException, RetrievalFailedException, SearchIndexException, UpsertFailedException}
import com.twosixlabs.dart.search.serialization.EsCdrDocument
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.TimeUnit.SECONDS
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

object ElasticsearchIndex {
    val DART_JSON = new DartJsonFormat
}

class ElasticsearchIndex( client : ElasticClient ) extends SearchIndex with JsonFormat {

    import com.twosixlabs.dart.search.SearchIndex._

    // TODO - handle in a more unified way
    implicit val executionContext = scala.concurrent.ExecutionContext.global

    implicit object IndexableAnnotation extends Indexable[ CdrAnnotation[ _ ] ] {
        override def json( a : CdrAnnotation[ _ ] ) : String = DART_JSON.marshalAnnotation( a ).get
    }

    private val LOG : Logger = LoggerFactory.getLogger( getClass )
    private val INDEX : String = "cdr_search"

    private val TENANTS_FIELD = "tenants"

    override def upsertDocument( doc : CdrDocument, tenants : Iterable[ CorpusTenant ] = Nil ) : Future[ Unit ] = {
        val cdrJson = DART_JSON.marshalCdr( doc ).get
        val tenantsJson = marshalFrom( tenants.map( _.id ).toList ).get

        val updateBody =
            s"""{
               |  "scripted_upsert": true,
               |  "upsert": {},
               |  "script": {
               |    "source": "ArrayList t = new ArrayList(); if (ctx._source.tenants != null) { t = ctx._source.tenants; } for (item in params.tenants) { if (!t.contains(item)) { t.add(item); } } ctx._source = params.doc; ctx._source.tenants = t;",
               |    "lang": "painless",
               |    "params": {
               |       "$TENANTS_FIELD": $tenantsJson,
               |       "doc": $cdrJson
               |    }
               |  }
               |}
               |""".stripMargin

        val request = ElasticRequest( "POST", s"/${INDEX}/_update/${doc.documentId}?retry_on_conflict=100", HttpEntity( updateBody, "application/json" ) )

        val promise = Promise[ HttpResponse ]()
        val callback : Either[ Throwable, HttpResponse ] => Unit = {
            case Left( t ) => promise.tryFailure( t )
            case Right( r ) => promise.trySuccess( r )
        }

        client.client.send( request, callback )

        promise
          .future
          .transform {
              case Success( r : HttpResponse ) => {
                  if( LOG.isDebugEnabled() ) LOG.debug( r.toString )
                  Success()
              }
              case Failure( e ) => Failure( new UpsertFailedException( doc.documentId, UPSERT_OPERATION, cause = e ) )
          }
    }

    override def updateAnnotation( doc : CdrDocument, annotation : CdrAnnotation[ _ ] ) : Future[ Unit ] = {
        LOG.debug( s"upsert annotation ${doc.documentId}-${annotation.label}" )

        // because of limitations in the elastic4s API we have to drop down into a raw ES query
        val annotationJson = DART_JSON.marshalAnnotation( annotation ).get

        val esUpdateRequest =
            s"""{
               |  "script": {
               |    "source": "if (ctx._source.annotations == null) { ctx._source.annotations = [ params.anno ]; } else { boolean updated = false; for (int i = 0; i < ctx._source.annotations.length; i++) { if (ctx._source.annotations[i].label == params.anno.label) { ctx._source.annotations[i] = params.anno; updated = true; } } if (!updated) { ctx._source.annotations.add(params.anno); } }",
               |    "lang": "painless",
               |    "params": {
               |      "anno": $annotationJson
               |    }
               |  }
               |}
               |""".stripMargin

        if ( LOG.isTraceEnabled ) LOG.trace( s"${esUpdateRequest}" )

        val request = ElasticRequest( "POST", s"/${INDEX}/_update/${doc.documentId}?retry_on_conflict=100", HttpEntity( esUpdateRequest, "application/json" ) )

        val promise = Promise[ HttpResponse ]()
        val callback : Either[ Throwable, HttpResponse ] => Unit = {
            case Left( t ) => promise.tryFailure( t )
            case Right( r ) => promise.trySuccess( r )
        }

        client.client.send( request, callback )

        //@formatter:off
        promise
          .future
          .transform {
              case Success( r : HttpResponse ) => {
                  if( LOG.isDebugEnabled() ) LOG.debug( r.toString )
                  Success()
              }
              case Failure( e ) => Failure( new UpsertFailedException( doc.documentId, operation = UPDATE_ANNOTATION_OP, cause = e ) )
          }
        //@formatter:on
    }

    def getDocument( docId : String ) : Future[ CdrDocument ] = {
        import com.sksamuel.elastic4s.ElasticDsl._
        //@formatter:off
        client.execute{ search( INDEX )
          .query( idsQuery( docId ) )
          .fetchSource( true )
          .size( 1 )
          .timeout( FiniteDuration( 4, SECONDS ) )
        }.transform {
            case Success( response : Response[ SearchResponse ] ) =>
                val emptyOptFailure = Failure( new CdrUnreadableException( docId, GET_OPERATION ) )
                // If NoSuchElementException
                val docJsonTry = Try( response.result.hits.hits.head.sourceAsString )
                  .recoverWith {
                      case e : NoSuchElementException => Failure( new CdrNotFoundException( docId, GET_OPERATION, e ) )
                  }
                ( for {
                    docJson <- docJsonTry
                    esDocDto <- unmarshalTo( docJson, classOf[ EsCdrDocument ] )
                      // If the esCdrDoc has no doc id, then the doc was never indexed -- only the tenant was
                      // added
                      .transform( esCdrDoc => {
                          if ( esCdrDoc.cdrDto.documentId == null )
                              Failure( new CdrNotFoundException( docId, GET_OPERATION ) )
                          else Success( esCdrDoc )
                      }, e => Failure( e ) )
                    cdrJson <- marshalFrom( esDocDto.cdrDto ) // Unlikely to fail
                    cdr <- Try( DART_JSON.unmarshalCdr( cdrJson ).foldLeft[ Try[ CdrDocument ]]( emptyOptFailure )( (_, cdr) => Success( cdr )) )
                      .flatten
                } yield cdr ) recoverWith {
                    case e : SearchIndexException => Failure( e ) // Errors already handled above
                    case _ => Failure( new RetrievalFailedException( docId, GET_OPERATION ) )
                }
            case Success( _ ) => Failure( new RetrievalFailedException( docId,  "no response", GET_OPERATION ) )
            case Failure( e ) => Failure( new RetrievalFailedException( docId, operation = GET_OPERATION, cause = e ) )
        }
        //@formatter:on
    }
}

