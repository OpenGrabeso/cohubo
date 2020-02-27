package com.github.opengrabeso.cohabo
package rest

import io.udash.rest._

import scala.concurrent.Future

trait UserRestSettingsAPI {
  @GET
  def quest_time_offset: Future[Int]

  @GET
  def max_hr: Future[Int]

  @GET
  def elev_filter: Future[Int]

  @PUT
  def quest_time_offset(v: Int): Future[Unit]

  @PUT
  def max_hr(v: Int): Future[Unit]

  @PUT
  def elev_filter(v: Int): Future[Unit]
}

object UserRestSettingsAPI extends RestApiCompanion[EnhancedRestImplicits,UserRestSettingsAPI](EnhancedRestImplicits)