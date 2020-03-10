package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.serialization.transientDefault

import scala.concurrent.Future
import common.model._
import io.udash.rest._

trait ThreadsAPI {
  @GET("")
  def get(id: Long): Future[Notification]

  def threads(threadId: Long): SubscriptionAPI
}

object ThreadsAPI extends RestClientApiCompanion[EnhancedRestImplicits,ThreadsAPI](EnhancedRestImplicits)
