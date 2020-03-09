package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.serialization.transientDefault

import scala.concurrent.Future
import common.model._
import io.udash.rest._

trait IssuesAPI {
  @GET
  def comments: Future[Seq[Comment]]
}

object IssuesAPI extends RestClientApiCompanion[EnhancedRestImplicits,IssuesAPI](EnhancedRestImplicits)
