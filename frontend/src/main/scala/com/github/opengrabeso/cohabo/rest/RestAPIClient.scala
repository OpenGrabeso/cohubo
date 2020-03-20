package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.misc.Opt
import com.avsystem.commons.rpc.AsReal
import com.avsystem.commons.serialization.GenCodec
import common.model._
import com.softwaremill.sttp._
import io.udash.rest.raw._
import io.udash.rest.{RestException, SttpRestClient}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object RestAPIClient {
  implicit val sttpBackend: SttpBackend[Future, Nothing] = SttpRestClient.defaultBackend()
  val api: RestAPI = SttpRestClient[RestAPI]("https://api.github.com")
  def apply(): RestAPI = api

  // adapted from io.udash.rest.SttpRestClient#fromSttpResponse
  // we cannot use it directly, as it is private there
  private def fromSttpResponse(sttpResp: Response[String]): RestResponse = RestResponse(
    sttpResp.code,
    IMapping(sttpResp.headers.iterator.map { case (n, v) => (n, PlainValue(v)) }.toList),
    sttpResp.contentType.fold(HttpBody.empty) { contentType =>
      val mediaType = HttpBody.mediaTypeOf(contentType)
      HttpBody.charsetOf(contentType) match {
        case Opt(charset) =>
          val text = sttpResp.body.fold(identity, identity)
          HttpBody.textual(text, mediaType, charset)
        case _ =>
          HttpBody.textual(sttpResp.unsafeBody, contentType)
      }
    }
  )

  // many APIs return URLs which should be used to obtain more information - this can be used to have the result decoded
  def request[T](uri: String, token: String, method: Method = Method.GET)(implicit asReal: AsReal[RestResponse, T]): Future[T] = {
    val request = sttp.method(method, uri"$uri").auth.bearer(token)
    sttpBackend.send(request).map { r =>
      val raw = fromSttpResponse(r)
      implicitly[AsReal[RestResponse, T]].asReal(raw)
    }
  }

  // used for issue paging - use the provided URL and process the result headers
  def requestWithHeaders[T: GenCodec](uri: String, token: String): Future[DataWithHeaders[Seq[T]]] = {
    val request = sttp.method(Method.GET, uri"$uri").auth.bearer(token)

    sttpBackend.send(request).map { r =>
      val raw = fromSttpResponse(r)
      import io.udash.rest.GenCodecRestImplicits._
      EnhancedRestImplicits.fromResponse[T].asReal(raw)
    }
  }
}
