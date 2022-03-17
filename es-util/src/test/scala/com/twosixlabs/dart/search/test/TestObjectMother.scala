package com.twosixlabs.dart.search.test

import better.files.File
import com.twosixlabs.cdr4s.core.{CdrDocument, CdrMetadata}
import com.twosixlabs.dart.utils.DatesAndTimes
import com.typesafe.config.{Config, ConfigFactory}

object TestObjectMother {

    val TEST_CONFIG : Config = ConfigFactory.parseFile( File( "src/test/resources/env/test.conf" ).toJava ).resolve()

    val VALID_FACTIVA_JSON : String =
        """{
          |  "copyright": "(c) Copyright 2018.  SKRIN.  All rights reserved. ",
          |  "subject_codes": ",nanl,ncat,",
          |  "art": "",
          |  "modification_datetime": "1536847807000",
          |  "body": "Foreign market\n\nWednesday, the buyers optimism won the emerging markets, To view the original PDF article, please click on the link below:\n\nhttp://www
          |  .skrin.ru/analytics/stats.asp?url=/analytics/reviews/documents/image002.jpg\u0026doc=E71A6D52FA264FD89C7E702FAACDB6C1\u0026author=432569A4004FCDE9432569DA0044A534",
          |  "company_codes_occur": "",
          |  "company_codes_about": ",rosgos,",
          |  "company_codes_lineage": "",
          |  "snippet": "DAILY BONDS REVIEW\n\n13 September 2018",
          |  "publication_date": "1536796800000",
          |  "market_index_codes": "",
          |  "credit": "",
          |  "section": "",
          |  "currency_codes": "",
          |  "region_of_origin": "ASIA EEURZ EUR RUSS USSRZ ",
          |  "ingestion_datetime": "1536847817000",
          |  "modification_date": "1541255000190",
          |  "source_name": "SKRIN Analytics",
          |  "language_code": "en",
          |  "region_codes": ",usa,balkz,",
          |  "company_codes_association": "",
          |  "person_codes": ",110451339,110451339,",
          |  "company_codes_relevance": ",eurcb,bnkeng,",
          |  "source_code": "SKRANE",
          |  "an": "SKRANE0020180913ee9d0002t",
          |  "word_count": "881",
          |  "company_codes": ",bnkeng,eurcb,rosgos,",
          |  "industry_codes": ",i1300006,isptech,",
          |  "title": "Veles Capital: Daily bonds review. Veles Capital IC",
          |  "publication_datetime": "1536796800000",
          |  "publisher_name": "SKRIN info s.r.o.",
          |  "action": "add",
          |  "byline": "By Gene Epstein",
          |  "document_type": "article"
          |}""".stripMargin


    val CDR_TEMPLATE : CdrDocument = CdrDocument( captureSource = "ManualCuration",
                                                  extractedMetadata = {
                                                      CdrMetadata( creationDate = DatesAndTimes.timeStamp.toLocalDate,
                                                                   modificationDate = DatesAndTimes.timeStamp.toLocalDate,
                                                                   author = "michael",
                                                                   docType = "pdf",
                                                                   description = "Lorum Ipsum",
                                                                   originalLanguage = "en",
                                                                   classification = "UNCLASSIFIED",
                                                                   title = "Lorum Ipsum",
                                                                   publisher = "Lorum Ipsum",
                                                                   url = "https://www.lorumipsum.com" )
                                                  },
                                                  contentType = "text/html",
                                                  extractedNumeric = Map.empty,
                                                  documentId = "123abc",
                                                  extractedText = "Lorum Ipsum",
                                                  uri = "https://lorumipsum.com",
                                                  sourceUri = "Lorum Ipsum",
                                                  extractedNtriples = "<http://graph.causeex.com/documents/sources#b73796720b6f469fe323bb49794a13b0>",
                                                  timestamp = DatesAndTimes.timeStamp,
                                                  annotations = List() )


}
