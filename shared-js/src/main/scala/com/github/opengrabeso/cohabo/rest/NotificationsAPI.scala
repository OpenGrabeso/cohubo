package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.serialization.transientDefault

import scala.concurrent.Future
import common.model._
import io.udash.rest._

trait NotificationsAPI {
  def threads: ThreadsAPI
}

object NotificationsAPI extends RestClientApiCompanion[EnhancedRestImplicits,NotificationsAPI](EnhancedRestImplicits)
