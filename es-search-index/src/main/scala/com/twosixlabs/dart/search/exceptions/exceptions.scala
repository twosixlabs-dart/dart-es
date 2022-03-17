package com.twosixlabs.dart.search

package object exceptions {

    class SearchIndexException( message : String, val operation : Option[ String ], cause : Throwable )
      extends Exception( message + ( if ( operation.isEmpty ) "" else s" (${operation.get})" ), cause ) {
        def this( msg : String, operation : String, cse : Throwable = null ) = {
            this( msg, if ( operation.isEmpty ) None else Some( operation ), cse )
        }
    }

    class UpsertFailedException( val docId : String, val reason : String = "unknown reason", operation : String = "", cause : Throwable = null )
      extends SearchIndexException( s"Unable to upsert $docId: ${reason}", operation, cause )

    class TenantNotFoundException( docId : String, tenantId : String, operation : String = "", cause : Throwable = null )
      extends UpsertFailedException( docId, s"tenant $tenantId does not exist", operation, cause )

    class RetrievalFailedException( val docId: String, val reason : String = "unknown reason", operation : String = "", cause : Throwable = null )
      extends SearchIndexException( s"Unable to retrieve $docId: ${reason}", operation, cause )

    class CdrNotFoundException( docId : String, operation : String = "", cause : Throwable = null )
      extends RetrievalFailedException( docId, s"not found", operation, cause )

    class CdrUnreadableException( docId : String, operation : String = "", cause : Throwable = null )
      extends RetrievalFailedException( docId, s"could not parse document", operation, cause )

}
