//package com.twosixlabs.dart.search
//
//import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex.{DocIdMissingFromTenantException, InvalidTenantIdException, TenantAlreadyExistsException, TenantNotFoundException}
//import com.twosixlabs.dart.auth.tenant.{CorpusTenant, CorpusTenantIndex, GlobalCorpus}
//import org.scalatest.BeforeAndAfterAll
//import org.scalatest.flatspec.AnyFlatSpecLike
//import org.scalatest.matchers.should.Matchers
//
//import scala.concurrent.duration._
//import scala.concurrent.{Await, ExecutionContext, Future}
//import scala.util.{Failure, Success, Try}
//
//abstract class CorpusTenantIndexTest( val index : CorpusTenantIndex ) extends AnyFlatSpecLike with BeforeAndAfterAll with Matchers {
//
//    val className = index.getClass.getSimpleName
//
//    implicit val ec : ExecutionContext = index.executionContext
//
//    import index.CorpusTenantWithDocuments
//
//    val writeDelay : Int = 1000
//
//    override def beforeAll( ) : Unit = {
//        super.beforeAll()
//
//        require( Await.result( index.allTenants, 5.seconds ).isEmpty )
//    }
//
//    val docIds : Seq[ String ] = Range( 1, 21 ).map( i => s"doc-id-$i" )
//    val originalTenants : Seq[ CorpusTenant ] = Range( 1, 4 ).map( i => CorpusTenant( s"tenant-id-$i", GlobalCorpus ) )
//
//    behavior of className
//
//    it should "add a new tenant" in {
//        Await.result( originalTenants.head.addToIndex(), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( index.allTenants, 5.seconds ) shouldBe Seq( originalTenants.head )
//        Await.result( index.tenant( originalTenants.head.id ), 5.seconds ) shouldBe originalTenants.head
//
//        Await.result( index.addTenants( originalTenants.slice( 1, 3 ).map( _.id ) ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( index.allTenants.map( _.toSet ), 5.seconds ) shouldBe originalTenants.toSet
//        Await.result( index.tenant( originalTenants.head.id ), 5.seconds ) shouldBe originalTenants.head
//        Await.result( index.tenant( originalTenants( 1 ).id ), 5.seconds ) shouldBe originalTenants( 1 )
//        Await.result( index.tenant( originalTenants( 2 ).id ), 5.seconds ) shouldBe originalTenants( 2 )
//    }
//
//    it should "add documents to a tenant" in {
//        Await.result( index.addDocumentToTenant( docIds.head, originalTenants.head ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( originalTenants.head.documents, 5.seconds ) shouldBe Seq( docIds.head )
//        Await.result( index.documentTenants( docIds.head ), 5.seconds ) shouldBe Seq( originalTenants.head )
//
//        Await.result( index.addDocumentsToTenant( docIds, originalTenants( 1 ).id ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( originalTenants( 1 ).documents.map( _.toSet ), 5.seconds ) shouldBe docIds.toSet
//        docIds.foreach( id => Await.result( index.documentTenants( id ), 5.seconds ).contains( originalTenants( 1 ) ) shouldBe true )
//
//        Await.result( index.addDocumentsToTenant( docIds.slice( 4, 10 ), originalTenants( 2 ).id ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( originalTenants( 2 ).documents.map( _.toSet ), 5.seconds ) shouldBe docIds.slice( 4, 10 ).toSet
//        docIds.slice( 4, 10 ).foreach { id =>
//            val res = Await.result( index.documentTenants( id ), 5.seconds )
//            res.contains( originalTenants( 2 ) ) shouldBe true
//            res.contains( originalTenants( 1 ) ) shouldBe true
//            res.contains( originalTenants.head ) shouldBe false
//        }
//    }
//
//    it should "remove documents from a tenant" in {
//        Await.result( index.removeDocumentsFromTenant( docIds.slice( 0, 7 ), originalTenants( 1 ).id ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( index.documentTenants( docIds.head ), 5.seconds ) shouldBe Seq( originalTenants.head )
//        Await.result( index.documentTenants( docIds( 1 ) ), 5.seconds ) shouldBe Seq()
//        Await.result( index.documentTenants( docIds( 2 ) ), 5.seconds ) shouldBe Seq()
//        Await.result( index.documentTenants( docIds( 3 ) ), 5.seconds ) shouldBe Seq()
//        Await.result( index.documentTenants( docIds( 4 ) ), 5.seconds ) shouldBe Seq( originalTenants( 2 ) )
//        Await.result( index.documentTenants( docIds( 5 ) ), 5.seconds ) shouldBe Seq( originalTenants( 2 ) )
//        Await.result( index.documentTenants( docIds( 6 ) ), 5.seconds ) shouldBe Seq( originalTenants( 2 ) )
//        Await.result( index.documentTenants( docIds( 7 ) ), 5.seconds ).toSet shouldBe originalTenants.slice( 1, 3 ).toSet
//        Await.result( index.documentTenants( docIds( 8 ) ), 5.seconds ).toSet shouldBe originalTenants.slice( 1, 3 ).toSet
//        Await.result( index.documentTenants( docIds( 9 ) ), 5.seconds ).toSet shouldBe originalTenants.slice( 1, 3 ).toSet
//    }
//
//    it should "remove a document from all tenants" in {
//        Await.result( index.addDocumentToTenant( "new-test-doc-id", originalTenants.head ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( index.addDocumentToTenant( "new-test-doc-id", originalTenants( 2 ) ), 5.seconds )
//        Thread.sleep( writeDelay )
//        val tenants = Await.result( index.documentTenants( "new-test-doc-id" ), 5.seconds )
//        tenants should ( contain( originalTenants.head ) and contain( originalTenants( 2 ) ) )
//        Await.result( index.removeDocumentFromIndex( "new-test-doc-id" ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( index.documentTenants( "new-test-doc-id" ), 5.seconds ) shouldBe Nil
//    }
//
//    it should "remove several documents from all tenants" in {
//        Await.result( index.addDocumentsToTenant( List( "new-test-doc-id-1", "new-test-doc-id-2" ), originalTenants.head.id ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( index.addDocumentsToTenant( List( "new-test-doc-id-2", "new-test-doc-id-3" ), originalTenants( 2 ).id ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( index.addDocumentsToTenant( List( "new-test-doc-id-1", "new-test-doc-id-3" ), originalTenants( 1 ).id ), 5.seconds )
//        Thread.sleep( writeDelay )
//        val tenants1 = Await.result( index.documentTenants( "new-test-doc-id-1" ), 5.seconds )
//        tenants1 should ( contain( originalTenants.head ) and contain( originalTenants( 1 ) ) )
//        val tenants2 = Await.result( index.documentTenants( "new-test-doc-id-2" ), 5.seconds )
//        tenants2 should ( contain( originalTenants.head ) and contain( originalTenants( 2 ) ) )
//        val tenants3 = Await.result( index.documentTenants( "new-test-doc-id-3" ), 5.seconds )
//        tenants3 should ( contain( originalTenants( 1 ) ) and contain( originalTenants( 2 ) ) )
//        Await.result( index.removeDocumentsFromIndex( List( "new-test-doc-id-1", "new-test-doc-id-2", "new-test-doc-id-3" ) ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( index.documentTenants( "new-test-doc-id-1" ), 5.seconds ).isEmpty shouldBe true
//        Await.result( index.documentTenants( "new-test-doc-id-2" ), 5.seconds ).isEmpty shouldBe true
//        Await.result( index.documentTenants( "new-test-doc-id-3" ), 5.seconds ).isEmpty shouldBe true
//    }
//
//    it should "remove a tenant" in {
//        Await.result( index.removeTenant( originalTenants( 2 ) ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( index.allTenants, 5.seconds ).toSet shouldBe originalTenants.slice( 0, 2 ).toSet
//        val docTenants = Await.result( Future.sequence( docIds.map( index.documentTenants ) ), 5.seconds ).flatten
//        docTenants should not contain ( originalTenants( 2 ) )
//        docTenants.map( _.id ) should not contain originalTenants( 2 ).id
//    }
//
//    it should "throw TenantNotFoundException when attempting to retrieve non-existing tenant by id" in {
//        Try( Await.result( index.tenant( "non-existent-fake-tenant" ), 5.seconds ) ) match {
//            case Failure( e : TenantNotFoundException ) => e.getMessage should include( "id: non-existent-fake-tenant" )
//            case Failure( e ) => fail( s"threw the wrong error: $e" )
//            case Success( res ) => fail( s"failed to fail: $res" )
//        }
//    }
//
//    it should "throw TenantNotFoundException when attempting to remove non-existing tenant" in {
//        Try( Await.result( index.removeTenant( "non-existent-fake-tenant" ), 5.seconds ) ) match {
//            case Failure( e : TenantNotFoundException ) => e.getMessage should include( "id: non-existent-fake-tenant" )
//            case Failure( e ) => fail( s"threw the wrong error: $e" )
//            case Success( res ) => fail( s"failed to fail: $res" )
//        }
//
//        Try( Await.result( index.removeTenant( "tenant-id-1", "non-existent-fake-tenant-1", "non-existent-fake-tenant-2", "tenant-id-3" ), 5.seconds ) ) match {
//            case Failure( e : TenantNotFoundException ) => {
//                e.getMessage should include( "id: non-existent-fake-tenant" )
//                noException should be thrownBy ( Await.result( index.tenant( "tenant-id-1" ), 5.second ) )
//            }
//            case Failure( e ) => fail( s"threw the wrong error: $e" )
//            case Success( res ) => fail( s"failed to fail: $res" )
//        }
//    }
//
//    it should "throw TenantNotFoundException when attempting to add documents to non-existing tenant" in {
//        Try( Await.result( index.addDocumentToTenant( "some-doc-id", "non-existent-fake-tenant" ), 5.seconds ) ) match {
//            case Failure( e : TenantNotFoundException ) => e.getMessage should include( "id: non-existent-fake-tenant" )
//            case Failure( e ) => fail( s"threw the wrong error: $e" )
//            case Success( res ) => fail( s"failed to fail: $res" )
//        }
//    }
//
//    it should "throw TenantAlreadyExistsException when attempting to add a tenant that already exists" in {
//        Try( Await.result( index.addTenant( "tenant-id-2" ), 5.seconds ) ) match {
//            case Failure( e : TenantAlreadyExistsException ) => {
//                e.getMessage should include( "id: tenant-id-2" )
//            }
//            case Failure( e ) => fail( s"threw the wrong error: $e" )
//            case Success( res ) => fail( s"failed to fail: $res" )
//        }
//
//        Try( Await.result( index.addTenants( List( "tenant-id-3", "non-existent-fake-tenant-1", "non-existent-fake-tenant-2", "tenant-id-2" ) ), 5.seconds ) ) match {
//            case Failure( e : TenantAlreadyExistsException ) => {
//                e.getMessage should include( "id: tenant-id-2" )
//                a[ TenantNotFoundException ] should be thrownBy ( Await.result( index.tenant( "tenant-id-3" ), 5.seconds ) )
//            }
//            case Failure( e ) => fail( s"threw the wrong error: $e" )
//            case Success( res ) => fail( s"failed to fail: $res" )
//        }
//    }
//
//    it should "throw InvalidTenantIdException when attempting to add tenant with invalid id" in {
//        Try( Await.result( index.addTenant( "tenant:id~2" ), 5.seconds ) ) match {
//            case Failure( e : InvalidTenantIdException ) => {
//                e.getMessage should include( "id tenant:id~2" )
//            }
//            case Failure( e ) => fail( s"threw the wrong error: $e" )
//            case Success( res ) => fail( s"failed to fail: $res" )
//        }
//
//        Try( Await.result( index.addTenants( List( "valid-tenant-1", "INVALID", "valid-tenant-2", "also###invalid" ) ), 5.seconds ) ) match {
//            case Failure( e : InvalidTenantIdException ) => {
//                e.getMessage should include( "id INVALID" )
//                a[ TenantNotFoundException ] should be thrownBy ( Await.result( index.tenant( "valid-tenant-1" ), 5.seconds ) )
//            }
//            case Failure( e ) => fail( s"threw the wrong error: $e" )
//            case Success( res ) => fail( s"failed to fail: $res" )
//        }
//    }
//
//    it should "throw DocIdMissingFromTenantException when attempting to delete non-existant document from a tenant" in {
//        Await.result( index.addTenant( "last-test-tenant" ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( index.addDocumentsToTenant( List( "d1", "d2", "d3" ), "last-test-tenant" ), 5.seconds )
//        Thread.sleep( writeDelay )
//        Await.result( index.tenantDocuments( "last-test-tenant" ), 5.seconds )
//        Try( Await.result( index.removeDocumentFromTenant( "d4", "last-test-tenant" ), 5.seconds ) ) match {
//            case Failure( e : DocIdMissingFromTenantException ) => {
//                e.getMessage should include( "d4 is not in tenant last-test-tenant" )
//            }
//            case Failure( e ) => fail( s"threw the wrong error: $e" )
//            case Success( res ) => fail( s"failed to fail: $res" )
//        }
//        Await.result( index.tenantDocuments( "last-test-tenant" ), 5.seconds )
//
//        Await.result( index.tenantDocuments( "last-test-tenant" ), 5.seconds )
//        Try( Await.result( index.removeDocumentsFromTenant( List( "d1", "d2", "d4", "d3", "d7" ), "last-test-tenant" ), 5.seconds ) ) match {
//            case Failure( e : DocIdMissingFromTenantException ) => {
//                e.getMessage should include( "d4 is not in tenant last-test-tenant" )
//                Await.result( index.tenantDocuments( "last-test-tenant" ), 5.seconds )
//                noException should be thrownBy ( Await.result( index.removeDocumentsFromTenant( List( "d1", "d2" ), "last-test-tenant" ), 5.seconds ) )
//            }
//            case Failure( e ) => fail( s"threw the wrong error: $e" )
//            case Success( res ) => fail( s"failed to fail: $res" )
//        }
//    }
//
//    override def afterAll( ) : Unit = {
//        Thread.sleep( writeDelay )
//        Await.result(
//            index.allTenants.flatMap( tenants => Future.sequence( tenants.map( tenant => tenant.removeFromIndex() ) ) ),
//            10.seconds,
//            )
//        Thread.sleep( writeDelay )
//
//        require( Await.result( index.allTenants, 5.seconds ).isEmpty )
//    }
//
//}
