package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.serialization.transientDefault

import scala.concurrent.Future
import common.model._
import io.udash.rest._

trait RepositoryAPI {
  @GET
  def issues(state: String = "open", @transientDefault creator: String = null): Future[Seq[Issue]]

  @Prefix("issues")
  def issuesAPI(number: Long): IssuesAPI
}

object RepositoryAPI extends RestApiCompanion[EnhancedRestImplicits,RepositoryAPI](EnhancedRestImplicits)
