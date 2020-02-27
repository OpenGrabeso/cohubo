package com.github.opengrabeso.cohabo
package rest

import com.softwaremill.sttp.SttpBackend
import io.udash.rest.SttpRestClient
import org.scalajs.dom

import scala.concurrent.Future
import scala.util.Try

object RestAPIClient {
  val api: RestAPI = {
    implicit val sttpBackend: SttpBackend[Future, Nothing] = SttpRestClient.defaultBackend()
    val (scheme, defaultPort) =
      if (dom.window.location.protocol == "https:") ("https", 443) else ("http", 80)
    val port = Try(dom.window.location.port.toInt).getOrElse(defaultPort)
    SttpRestClient[RestAPI](s"$scheme://${dom.window.location.hostname}:$port/rest")
  }
  def apply(): RestAPI = api
}
