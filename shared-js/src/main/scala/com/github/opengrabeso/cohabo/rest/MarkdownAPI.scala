package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.serialization.transientDefault

import scala.concurrent.Future
import common.model._
import io.udash.rest._

trait MarkdownAPI {
  @POST
  def markdown(@CustomBody text: String, @transientDefault mode: String = "markdown", context: String = ""): Future[TextData]
}

object MarkdownAPI extends RestApiCompanion[EnhancedRestImplicits,MarkdownAPI](EnhancedRestImplicits)

