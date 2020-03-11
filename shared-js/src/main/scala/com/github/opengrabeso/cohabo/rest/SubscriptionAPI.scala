package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.serialization.transientDefault

import scala.concurrent.Future
import common.model._
import io.udash.rest._

trait SubscriptionAPI {
  @GET("")
  def get: Future[ThreadSubscription]

  @PUT("")
  def set(ignored: Boolean): Future[Unit]

  @DELETE(path = "")
  def delete(): Future[Unit]
}

object SubscriptionAPI extends RestClientApiCompanion[EnhancedRestImplicits,SubscriptionAPI](EnhancedRestImplicits)
