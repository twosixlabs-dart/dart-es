package com.twosixlabs.dart.search

import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.script.Script
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.term.TermQuery
import com.sksamuel.elastic4s.requests.update.UpdateByQueryResponse
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties, RequestFailure, RequestSuccess, Response}
import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex.{DocIdAlreadyInTenantException, DocIdMissingFromIndexException, DocIdMissingFromTenantException, InvalidTenantIdException, TenantAlreadyExistsException, TenantNotFoundException}
import com.twosixlabs.dart.auth.tenant.{CorpusTenant, CorpusTenantIndex}
import com.twosixlabs.dart.auth.utilities.AsyncUtils.retry
import com.typesafe.config.Config

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class ElasticsearchCorpusTenantIndex( dependencies : ElasticsearchCorpusTenantIndex.Dependencies )
  extends CorpusTenantIndex {

    import dependencies._

    override implicit val executionContext : ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    val CDR_INDEX : String = "cdr_search"
    val TENANT_INDEX : String = "tenants"
    val TENANTS_FIELD : String = "tenants"
    val TENANT_ID_FIELD : String = "tenant_id"
    val DOC_ID_SEARCH_FIELD : DocId = "document_id.term"
    val DOC_ID_RESULT_FIELD : String = "document_id"

    val maxSize : Int = 10000

    val url : String = s"${scheme}://${host}:${port}"

    val client : ElasticClient = ElasticClient( JavaClient( ElasticProperties( url ) ) )

    override def allTenants : Future[ Seq[ CorpusTenant ] ] = {
        import com.sksamuel.elastic4s.ElasticDsl._
        
        val resFuture = client.execute(
            search( TENANT_INDEX ).query( existsQuery( TENANT_ID_FIELD ) ).size( maxSize )
        )

        resFuture.map( ( v : Response[ SearchResponse ] ) => {
            v.result.hits.hits.map( d => CorpusTenant( d.sourceAsMap( TENANT_ID_FIELD ).toString ) )
        } )
    }

    override def tenant( tenantId : TenantId ) : Future[ CorpusTenant ] = {
        import com.sksamuel.elastic4s.ElasticDsl._

        val resFuture = client.execute(
            search( TENANT_INDEX ).query( termQuery( TENANT_ID_FIELD, tenantId ) )
        )

        resFuture.flatMap( ( v : Response[ SearchResponse ] ) => {
            v.result.hits.hits.headOption match {
                case Some( hit ) =>
                    Future.successful( CorpusTenant( hit.sourceAsMap( TENANT_ID_FIELD ).toString ) )
                case None => Future.failed( new TenantNotFoundException( tenantId ) )
            }
        } )
    }

    override def tenantDocuments( tenantId : TenantId ) : Future[ Seq[ DocId ] ] = {
        import com.sksamuel.elastic4s.ElasticDsl._

        val checkFuture = tenant( tenantId )

        val resFuture = checkFuture flatMap { tenant =>
            client.execute(
                search( CDR_INDEX )
                  .query( termQuery( TENANTS_FIELD, tenant.id ) )
                  .size( maxSize )
            )
        }

        resFuture.map( ( v : Response[ SearchResponse ] ) => {
            v.result.hits.hits.map( d => d.sourceAsMap( DOC_ID_RESULT_FIELD ).toString )
        } )
    }

    override def documentTenants( docId : DocId ) : Future[ Seq[ CorpusTenant ] ] = {
        import com.sksamuel.elastic4s.ElasticDsl._

        val resFuture = client.execute(
            search( CDR_INDEX )
              .query( termQuery( DOC_ID_SEARCH_FIELD, docId ) )
              .size( maxSize )
        )

        resFuture.flatMap( ( v : Response[SearchResponse ] ) => {
            v.result.hits.hits.headOption.map( d => {
                val tenants : List[ String ] = d.sourceAsMap( TENANTS_FIELD ).asInstanceOf[ List[ String ] ]
                Future.successful( tenants.map( CorpusTenant( _ ) ) )
            } ).getOrElse( Future.failed( new DocIdMissingFromIndexException( docId ) ) )
        } )
    }

    override def addTenants( tenantIds : Iterable[ TenantId ] ) : Future[ Unit ] = {
        import com.sksamuel.elastic4s.ElasticDsl._

        for {
            _ <- Future.sequence( tenantIds.map {
                case ValidTenant( validId ) => Future.successful( validId )
                case badId => Future.failed( new InvalidTenantIdException( badId ) )
            }  )
            _ <- Future.sequence( tenantIds.map( tId => tenant( tId ) transform {
                case Success( tenant ) => Failure( new TenantAlreadyExistsException( tenant.id ) )
                case Failure( _ : TenantNotFoundException ) => Success()
                case Failure( e ) => Failure( new Exception( s"Unable to check existence of tenant $tId", e )  )
            } ) )
            _ <- Future.sequence( tenantIds.map( tId => {
                client.execute(
                    indexInto( TENANT_INDEX ).fields( TENANT_ID_FIELD -> tId )
                ) map ( _ => () )
            } ) )
        } yield ()
    }

    override def removeTenants( tenantIds : Iterable[ TenantId ] ) : Future[ Unit ] = {
        val tenantsSet = tenantIds.toSet

        import com.sksamuel.elastic4s.ElasticDsl._
        for {
            _ <- Future.sequence( tenantIds.map( tId => tenant( tId ) transform {
                case Success( _ ) => Success()
                case Failure( _ : TenantNotFoundException ) => Failure( new TenantNotFoundException( tId ) )
                case Failure( e ) => Failure( new Exception( s"Unable to check existence of tenant $tId", e )  )
            } ) )
            docMap <- Future.sequence( tenantIds.map( tId => {
                    tenantDocuments( tId ).map( _.toSet )
                } ) ).map( _.foldLeft( Set.empty[ DocId ] )( _ ++ _ ) )
              .flatMap( docIds => Future.sequence( docIds.map( dId => {
                  documentTenants( dId ).map( ts => (dId, ts.map( _.id ).toSet -- tenantsSet) )
              } ) ) )
            _ <- Future.sequence(
                docMap.map { case (docId, ts) =>
                    client.execute {
                        updateById( CDR_INDEX, docId ).doc(
                            TENANTS_FIELD -> ts.toArray
                        )
                    }
                }
            )
            _ <- Future.sequence {
                tenantIds.map( tId => {
                    client.execute( deleteIn( TENANT_INDEX ).by( termQuery( TENANT_ID_FIELD, tId ) ) )
                } )
            }
        } yield ()
    }

    override def addDocumentsToTenants( docIds : Iterable[ DocId ],
                                        tenantIds : Iterable[ TenantId ] ) : Future[ Unit ] = {
        import com.sksamuel.elastic4s.ElasticDsl._

        for {
            _ <- Future.sequence( tenantIds.map( tId => tenantDocuments( tId ) transform {
                case Success( docs ) => docIds.find( dId => docs.contains( dId ) ) match {
                    case Some( dId ) => Failure( new DocIdAlreadyInTenantException( dId, tId ) )
                    case None => Success( (tId, docs) )
                }
                case Failure( _ : TenantNotFoundException ) => Failure( new TenantNotFoundException( tId ) )
                case Failure( e ) => Failure( new Exception( s"Unable to check existence of tenant $tId", e )  )
            } ) ) map ( _.toMap )
            _ <- Future.sequence( for {
                dId <- docIds
            } yield {
                client.execute {
                    updateIn( CDR_INDEX )
                      .query( termQuery( DOC_ID_SEARCH_FIELD, dId ) ) script {
                        script( s"for (tId in params.tenantIds) { ctx._source.${TENANTS_FIELD}.add(tId) }" )
                          .params( "tenantIds" -> tenantIds )
                    }
                } transformWith {
                    case Success( res : Response[UpdateByQueryResponse ] ) if ( res.result.updated == 0 ) =>
                        Future.failed( throw new DocIdMissingFromIndexException( dId ) )
                    case Success( _ ) => Future.successful()
                    case Failure( e ) => Future.failed( e )
                }
            } )
        } yield ()
    }

    private def docQuery( dId : String ) : TermQuery = {
        import com.sksamuel.elastic4s.ElasticDsl._

        termQuery( DOC_ID_SEARCH_FIELD, dId )
    }

    private def docTenantQuery( tId : String, dId : String ) : BoolQuery = {
        import com.sksamuel.elastic4s.ElasticDsl._

        boolQuery().filter( termQuery( DOC_ID_SEARCH_FIELD, dId ), termQuery( TENANTS_FIELD, tId ) )
    }

    override def removeDocumentsFromTenants( docIds : Iterable[ DocId ],
                                             tenantIds : Iterable[ TenantId ] ) : Future[ Unit ] = {
        import com.sksamuel.elastic4s.ElasticDsl._

        for {
            _ <- Future.sequence( tenantIds.map( tId => tenantDocuments( tId ) transform {
                case Success( docs ) => docIds.find( dId => !docs.toSet.contains( dId ) ) match {
                    case Some( dId ) => Failure( new DocIdMissingFromTenantException( tId, dId ) )
                    case None => Success()
                }
                case fail@Failure( _ : TenantNotFoundException ) => fail
                case Failure( e ) => Failure( new Exception( s"Unable to check existence of tenant $tId", e )  )
            } ) )
            _ <- Future.sequence( for {
                dId <- docIds
            } yield client.execute {
                updateIn( CDR_INDEX )
                  .query( docQuery( dId ) ) script {
                    script( s"for (tId in params.tenantIds) { int tIdIndex = ctx._source.${TENANTS_FIELD}.indexOf(tId); if (tIdIndex >= 0 && tIdIndex < ctx._source.${TENANTS_FIELD}.length) { ctx._source.${TENANTS_FIELD}.remove(tIdIndex) } }" )
                      .params( "tenantIds" -> tenantIds )
                }
            } transform {
                case Success( res : RequestSuccess[ UpdateByQueryResponse ] ) =>
                    if ( res.result.updated == 1 ) Success()
                    else Failure( new Exception( s"Unable to remove document ${dId} from tenants ${tenantIds}") )
                case Success( res : RequestFailure ) =>
                    Failure( new Exception( res.body.getOrElse( "" ) ) )
                case Failure( e ) => Failure( e )
            } )
        } yield ()
    }

    override def removeDocumentsFromIndex( docIds : Iterable[ DocId ] ) : Future[ Unit ] = {
        for {
            docMap <- Future.sequence( docIds.map( dId => documentTenants( dId ).map( tenants => (dId, tenants) ) ) )
                .transform {
                    case Success( docMap ) => docMap.find( _._2.isEmpty ) match {
                        case Some( (dId, _) ) => Failure( new DocIdMissingFromIndexException( dId ) )
                        case None => Success( docMap )
                    }
                }
            _ <- Future.sequence( docMap.map( tup => {
                val (dId, tenants) = tup
                removeDocumentFromTenants( dId, tenants.map( _.id ) )
            } ) )
        } yield ()
    }

    override def cloneTenant(
        existingTenant : TenantId,
        newTenant : TenantId ) : Future[ Unit ] = {
        import com.sksamuel.elastic4s.ElasticDsl._

        for {
            confirmedTenant <- tenant( existingTenant )
            _ <- addTenant( newTenant )
            docIds <- tenantDocuments( confirmedTenant )
            docMap <-  Future.sequence( docIds.map( dId => {
                  documentTenants( dId ).map( ts => (dId, ts.map( _.id ).toSet + newTenant ) )
              } ) )
            _ <- Future.sequence(
                docMap.map { case (docId, ts) =>
                    client.execute {
                        updateById( CDR_INDEX, docId ).doc(
                            TENANTS_FIELD -> ts.toArray
                        )
                    }
                }
            )
        } yield ()
    }
}

object ElasticsearchCorpusTenantIndex {
    trait Dependencies {
        val scheme : String
        val host : String
        val port : Int

        lazy val elasticsearchTenantIndex : ElasticsearchCorpusTenantIndex = {
            buildElasticsearchTenantIndex
        }

        def buildElasticsearchTenantIndex : ElasticsearchCorpusTenantIndex = {
            new ElasticsearchCorpusTenantIndex( this )
        }
    }

    def apply(
        host : String,
        port : Int = 9200,
        scheme : String = "http",
    ) : ElasticsearchCorpusTenantIndex = {
        val s = scheme; val h = host; val p = port
        new Dependencies {
            override val scheme : String = s
            override val host : String = h
            override val port : Int = p
        } buildElasticsearchTenantIndex
    }

    def apply( config : Config ) : ElasticsearchCorpusTenantIndex = apply(
        config.getString( "elasticsearch.host" ),
        config.getInt( "elasticsearch.port" ),
        config.getString( "elasticsearch.scheme" ),
    )
}
