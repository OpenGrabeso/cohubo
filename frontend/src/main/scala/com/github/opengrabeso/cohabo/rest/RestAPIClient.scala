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
    SttpRestClient[RestAPI]("https://api.github.com")
  }
  def apply(): RestAPI = api
}
