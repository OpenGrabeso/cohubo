package com.github.opengrabeso.cohabo
package rest

import common.model._
import io.udash.rest._

import scala.concurrent.Future

trait AuthorizedAPI {
  @GET
  def user: Future[User]
}

object AuthorizedAPI extends RestApiCompanion[EnhancedRestImplicits,AuthorizedAPI](EnhancedRestImplicits)