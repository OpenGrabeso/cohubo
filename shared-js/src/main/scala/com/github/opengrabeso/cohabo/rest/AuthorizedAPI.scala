package com.github.opengrabeso.cohabo
package rest

import common.model._
import io.udash.rest._

import scala.concurrent.Future

trait AuthorizedAPI {
  @GET
  def user: Future[User]

  def repos(owner: String, repo: String): RepositoryAPI

  @Prefix("")
  def markdown: MarkdownAPI
}

object AuthorizedAPI extends RestApiCompanion[EnhancedRestImplicits,AuthorizedAPI](EnhancedRestImplicits)