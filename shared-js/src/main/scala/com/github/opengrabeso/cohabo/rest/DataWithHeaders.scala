package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.rpc.AsReal
import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.json.{JsonReader, JsonStringInput}
import common.model._
import io.udash.rest.raw.{HttpBody, RestResponse}

case class DataWithHeaders[T](data: T, paging: Map[String, String])

object DataWithHeaders {

  trait Implicits {

    // https://developer.github.com/v3/guides/traversing-with-pagination/
    /*
    Link: <https://api.github.com/search/code?q=addClass+user%3Amozilla&per_page=50&page=2>; rel="next",
    <https://api.github.com/search/code?q=addClass+user%3Amozilla&per_page=50&page=20>; rel="last"
    */
    def linkHeaders(linkHeader: Option[String]): Map[String, String] = {
      linkHeader.map { l =>
        val extract = """<([^>]*)>; *rel="([^"]*)"""".r
        extract.findAllMatchIn(l).map { m =>
          m.group(2) -> m.group(1)
        }.toMap
      }.getOrElse(Map.empty)
    }
    def fromString[T: GenCodec](text: String, linkHeader: Option[String]): DataWithHeaders[T] = {
      val codec = implicitly[GenCodec[T]]
      val input = new JsonStringInput(new JsonReader(text))
      val issues = codec.read(input)

      DataWithHeaders(issues, linkHeaders(linkHeader))
    }


    implicit def asResponse[T](implicit fromBody: AsReal[HttpBody, Seq[T]]): AsReal[RestResponse, DataWithHeaders[Seq[T]]] = AsReal.create {
      resp =>
        DataWithHeaders(fromBody.asReal(resp.body), linkHeaders(resp.headers.lift("link").map(_.value)))
    }
    // note: if OpenAPI is required, we should implement restResponses
  }

}


