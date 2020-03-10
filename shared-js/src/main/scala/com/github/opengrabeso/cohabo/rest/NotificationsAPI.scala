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
    @transientDefault all: Boolean = false,
    @transientDefault participating: Boolean = false,
    @transientDefault since: ZonedDateTime = null,
    @transientDefault before: ZonedDateTime = null
  ): Future[DataWithHeaders[Seq[Notification]]]

  def threads: ThreadsAPI
}

object NotificationsAPI extends RestClientApiCompanion[EnhancedRestImplicits,NotificationsAPI](EnhancedRestImplicits)
