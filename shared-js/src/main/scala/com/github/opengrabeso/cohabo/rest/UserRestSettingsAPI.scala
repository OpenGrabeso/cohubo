package com.github.opengrabeso.cohabo
package rest

import io.udash.rest._

import scala.concurrent.Future

trait UserRestSettingsAPI {
  @GET
  def user: Future[String]

  @GET
  def organization: Future[String]

  @GET
  def repository: Future[String]

  @PUT
  def user(v: String): Future[Unit]

  @PUT
  def organization(v: String): Future[Unit]

  @PUT
  def repository(v: String): Future[Unit]
}

object UserRestSettingsAPI extends RestApiCompanion[EnhancedRestImplicits,UserRestSettingsAPI](EnhancedRestImplicits)