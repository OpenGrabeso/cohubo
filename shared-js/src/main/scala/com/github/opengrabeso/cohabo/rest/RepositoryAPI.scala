package com.github.opengrabeso.cohabo
package rest

import java.time.ZonedDateTime

import com.avsystem.commons.serialization._

import scala.concurrent.Future
import common.model._
import io.udash.rest._

trait RepositoryAPI {
  @GET
  def issues(state: String = "open", @transientDefault creator: String = null): Future[DataWithHeaders[Seq[Issue]]]

  @GET
  def notifications(
    @transientDefault @Header("If-Modified-Since") ifModifiedSince: String = null,
    @transientDefault all: Boolean = false,
    @transientDefault participating: Boolean = false,
    @transientDefault since: ZonedDateTime = null,
    @transientDefault before: ZonedDateTime = null
  ): Future[DataWithHeaders[Seq[Notification]]]


  @Prefix("issues")
  def issuesAPI(number: Long): IssuesAPI
}

object RepositoryAPI extends RestClientApiCompanion[EnhancedRestImplicits,RepositoryAPI](EnhancedRestImplicits)
