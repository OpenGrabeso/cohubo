package com.github.opengrabeso.cohabo
package rest

import java.time.ZonedDateTime

import com.avsystem.commons.serialization.transientDefault

import scala.concurrent.Future
import common.model._
import io.udash.rest._

trait NotificationsAPI {
  @GET("")
  def get(
    @transientDefault @Header("If-Modified-Since") ifModifiedSince: String = null,
    @transientDefault all: Boolean = false,
    @transientDefault participating: Boolean = false,
    @transientDefault since: ZonedDateTime = null,
    @transientDefault before: ZonedDateTime = null
  ): Future[DataWithHeaders[Seq[Notification]]]

  @PUT("")
  def markAsRead(@transientDefault last_read_at: ZonedDateTime = null): Future[Unit]

  def threads(threadId: Long): ThreadsAPI
}

object NotificationsAPI extends RestClientApiCompanion[EnhancedRestImplicits,NotificationsAPI](EnhancedRestImplicits)
