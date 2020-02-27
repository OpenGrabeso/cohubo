package com.github.opengrabeso.cohabo
package rest

import java.time.ZonedDateTime

import com.github.opengrabeso.cohabo.common.model.BinaryData
import io.udash.rest._

import scala.concurrent.Future

trait RestAPI {

  @GET
  def identity(@Path in: String): Future[String]

  /* Caution: cookie parameters are used from the dom.document when called from Scala.js */
  @Prefix("user")
  def userAPI(@Path userId: String, @Cookie authCode: String, @Cookie sessionId: String): UserRestAPI

  @GET
  def now: Future[ZonedDateTime]

  // create a limited session (no Strava access) - used for push uploader
  def uploadSession(userId: String, authCode: String, version: String): Future[String]

  // create even more limited session - used for push uploader error reporting
  def reportUploadSessionError(userId: String, authCode: String): Future[String]
}

object RestAPI extends RestApiCompanion[EnhancedRestImplicits,RestAPI](EnhancedRestImplicits) {
  final val apiVersion = "1.0"
}
