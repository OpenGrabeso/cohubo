package com.github.opengrabeso.cohabo
package rest

import java.time.ZonedDateTime

import com.avsystem.commons.serialization._

import scala.concurrent.Future
import common.model._
import io.udash.rest._

trait RepositoryAPI {
  @GET
  def issues(
    @transientDefault milestone: Int = -1,
    @transientDefault state: String = "open", // Can be either open, closed, or all. Default: open
    @transientDefault assignee: String = null,
    @transientDefault creator: String = null,
    @transientDefault mentioned: String = null,
    @transientDefault labels: String = null, // A list of comma separated label names
    @transientDefault sort: String = "created", // What to sort results by. Can be either created, updated, comments. Default: created
    @transientDefault direction: String = "desc", // The direction of the sort. Can be either asc or desc. Default: desc
    @transientDefault since: ZonedDateTime = null
  ): Future[DataWithHeaders[Seq[Issue]]]

  @POST("issues")
  def createIssue(
    title: String,
    body: String,
    @transientDefault milestone: Int = -1,
    @transientDefault labels: Array[String] = Array.empty,
    @transientDefault assignees: Array[String] = Array.empty
  ): Future[Issue]

  @GET
  def notifications(
    @transientDefault @Header("If-Modified-Since") ifModifiedSince: String = null,
    @transientDefault all: Boolean = false,
    @transientDefault participating: Boolean = false,
    @transientDefault since: ZonedDateTime = null,
    @transientDefault before: ZonedDateTime = null,
    noCache: String = ZonedDateTime.now.toEpochSecond.toString // prevent any other caching (esp. prevent Chrome adding "If-Modified-Since" headers)
  ): Future[DataWithHeaders[Seq[Notification]]]


  @Prefix("issues")
  def issuesAPI(number: Long): IssuesAPI

  @PATCH("issues/comments")
  def editComment(
    @Path
    id: Long,
    body: String
  ): Future[Comment]

}

object RepositoryAPI extends RestClientApiCompanion[EnhancedRestImplicits,RepositoryAPI](EnhancedRestImplicits)
