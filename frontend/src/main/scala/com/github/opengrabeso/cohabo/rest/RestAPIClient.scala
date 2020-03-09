package com.github.opengrabeso.cohabo
package rest

import com.softwaremill.sttp._
import io.udash.rest.SttpRestClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object RestAPIClient {
  implicit val sttpBackend: SttpBackend[Future, Nothing] = SttpRestClient.defaultBackend()
  val api: RestAPI = {
    SttpRestClient[RestAPI]("https://api.github.com")
  }
  def apply(): RestAPI = api

  // used for issue paging
  def requestIssues(uri: String, token: String) = {
    val request = sttp.method(Method.GET, Uri(uri)).header("Authorization", s"Bearer $token")

    sttpBackend.send(request).map { r =>
      r.body.map { resp =>
        IssuesWithHeaders.fromString(resp, r.headers.toMap.getOrElse("Link", ""))
      }
    }
  }
}
