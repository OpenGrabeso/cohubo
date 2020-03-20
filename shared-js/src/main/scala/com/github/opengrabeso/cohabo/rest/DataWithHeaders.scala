package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.rpc.AsReal
import io.udash.rest.raw.{HttpBody, HttpErrorException, RestResponse}

final case class DataWithHeaders[T](data: T, headers: DataWithHeaders.Headers = DataWithHeaders.Headers())

object DataWithHeaders {

  final case class Headers(
    paging: Map[String, String] = Map.empty,
    lastModified: Option[String] = None,
    xPollInterval: Option[String] = None
  )

  final case class HttpErrorExceptionWithHeaders(error: HttpErrorException, headers: Headers) extends Exception {
    override def toString = error.toString
    override def getMessage = error.getMessage
    override def getCause = error.getCause
    override def getStackTrace = error.getStackTrace
    override def getLocalizedMessage = error.getLocalizedMessage
  }


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

  def headersFromResponse(resp: RestResponse) = {
    def getHeader(name: String) = resp.headers.lift(name).map(_.value)
    Headers(
      linkHeaders(getHeader("link")),
      getHeader("last-modified"),
      getHeader("X-Poll-Interval")
    )
  }

  trait Implicits {

    implicit def fromResponse[T](implicit fromBody: AsReal[HttpBody, Seq[T]]): AsReal[RestResponse, DataWithHeaders[Seq[T]]] = AsReal.create {
      resp =>
        if (resp.isSuccess) {
          DataWithHeaders(fromBody.asReal(resp.body), headersFromResponse(resp))
        } else {
          throw HttpErrorExceptionWithHeaders(resp.toHttpError, headersFromResponse(resp))
        }

    }
  }

}


