package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.serialization._

import scala.concurrent.Future
import common.model._
import io.udash.rest._

trait RepositoryAPI {
  @GET
  def issues(state: String = "open", @transientDefault creator: String = null): Future[DataWithHeaders[Seq[Issue]]]

  @Prefix("issues")
  def issuesAPI(number: Long): IssuesAPI
}

object RepositoryAPI extends RestClientApiCompanion[EnhancedRestImplicits,RepositoryAPI](EnhancedRestImplicits)
