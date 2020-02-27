package com.github.opengrabeso.cohabo
package rest

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}

import com.avsystem.commons.serialization.whenAbsent
import common.model._
import io.udash.rest._
import io.udash.rest.raw.HttpBody

import scala.concurrent.Future

trait UserRestAPI {
  def logout: Future[Unit]

  def settings: UserRestSettingsAPI

  @GET("settings")
  def allSettings: Future[SettingsStorage]

  @GET
  def name: Future[String]

  @GET
  def stagedActivities(@Query @whenAbsent(Instant.ofEpochMilli(0).atZone(ZoneOffset.UTC)) notBefore: ZonedDateTime): Future[Seq[ArticleId]]

  def importFromStrava(stravaId: Long): Future[ArticleId]
}

object UserRestAPI extends RestApiCompanion[EnhancedRestImplicits,UserRestAPI](EnhancedRestImplicits)