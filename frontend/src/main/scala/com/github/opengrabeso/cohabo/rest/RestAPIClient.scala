package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.serialization.GenCodec
import common.model._
import com.softwaremill.sttp._
import io.udash.rest.{RestException, SttpRestClient}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object RestAPIClient {
  implicit val sttpBackend: SttpBackend[Future, Nothing] = SttpRestClient.defaultBackend()
  val api: RestAPI = {
    SttpRestClient[RestAPI]("https://api.github.com")
  }
  def apply(): RestAPI = api

  // used for issue paging
  def requestPage[T: GenCodec](uri: String, token: String): Future[DataWithHeaders[T]] = {
    println(s"requestIssues $uri")
    val request = sttp.method(Method.GET, uri"$uri").auth.bearer(token)

    sttpBackend.send(request).map { r =>
      r.body match {
        case Left(err) =>
          throw new RestException(err)
        case Right(resp) =>
          val headers = r.headers.toMap
          EnhancedRestImplicits.fromString[T](resp, headers.get("link"), headers.get("last-modified"))
      }
    }
  }
}
