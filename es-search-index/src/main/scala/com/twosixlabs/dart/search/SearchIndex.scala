package com.twosixlabs.dart.search

import com.twosixlabs.cdr4s.core.{CdrAnnotation, CdrDocument}
import com.twosixlabs.dart.auth.tenant.CorpusTenant

import scala.concurrent.Future

object SearchIndex {
    val UPSERT_OPERATION = "SearchIndex.upsertDocument"
    val UPDATE_ANNOTATION_OP = "SearchIndex.updateAnnotation"
    val GET_OPERATION = "SearchIndex"
}

trait SearchIndex {
    def upsertDocument( doc : CdrDocument, tenants : Iterable[ CorpusTenant ] = Nil ) : Future[ Unit ]

    def updateAnnotation( doc : CdrDocument, annotation : CdrAnnotation[ _ ] ) : Future[ Unit ]

    def getDocument( docId : String ) : Future[ CdrDocument ]
}

