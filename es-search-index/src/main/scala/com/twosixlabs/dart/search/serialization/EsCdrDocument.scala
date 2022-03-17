package com.twosixlabs.dart.search.serialization

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty, JsonUnwrapped}
import com.twosixlabs.cdr4s.json.dart.DartCdrDocumentDto

// Must use horrible mutable class to use JsonUnwrapped to unmarshal distinct objects from
// flat ES index
@JsonInclude( Include.NON_EMPTY )
class EsCdrDocument {
    @JsonUnwrapped
    var cdrDto : DartCdrDocumentDto = _

    @JsonProperty( "tenants" )
    var tenants : List[ String ] = _
}