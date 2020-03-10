package com.github.opengrabeso.cohabo
package rest

import java.time.ZonedDateTime

import io.udash.rest._
import common.model._

import scala.concurrent.Future

trait RestAPI {
  @GET(path="")
  def api: Future[APIRoot]

  @Prefix("")
  def authorized(@Header("Authorization") bearer: String): AuthorizedAPI
}

object RestAPI extends RestClientApiCompanion[EnhancedRestImplicits,RestAPI](EnhancedRestImplicits)
