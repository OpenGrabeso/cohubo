package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.rpc.AsReal
import io.udash.rest.raw.{HttpBody, RestResponse}

case class DataWithHeaders[T](data: T, paging: Map[String, String], lastModified: Option[String])

object DataWithHeaders {

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

  trait Implicits {


    implicit def fromResponse[T](implicit fromBody: AsReal[HttpBody, Seq[T]]): AsReal[RestResponse, DataWithHeaders[Seq[T]]] = AsReal.create {
      resp =>
        DataWithHeaders(
          fromBody.asReal(resp.ensureNonError.body),
          linkHeaders(resp.headers.lift("link").map(_.value)),
          resp.headers.lift("last-modified").map(_.value)
        )

    }
  }

}


