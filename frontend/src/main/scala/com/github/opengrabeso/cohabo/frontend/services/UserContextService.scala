package com.github.opengrabeso.cohabo
package frontend
package services

import common.model._
import common.Util._

import scala.concurrent.{ExecutionContext, Future}
import UserContextService._
import com.github.opengrabeso.cohabo.frontend.dataModel
import com.github.opengrabeso.cohabo.frontend.dataModel._
import com.github.opengrabeso.cohabo.rest.AuthorizedAPI
import io.udash.properties.model.ModelProperty
import org.scalajs.dom

object UserContextService {
  final val normalCount = 15

  case class LoadedActivities(staged: Seq[ArticleId])

  class UserContextData(token: String, rpc: rest.RestAPI)(implicit ec: ExecutionContext) {

    def api: AuthorizedAPI = rpc.authorized("Bearer " + token)

    def loadCached(): Future[LoadedActivities] = {
      Future.successful {
        LoadedActivities(
          for (i <- 1 to 10; j <- None +: (1000 to 1002).map(Some.apply) ) yield ArticleId(i.toString, j.map(_.toString))
        )
      }
    }
  }
}

class UserContextService(rpc: rest.RestAPI)(implicit ec: ExecutionContext) {

  val properties = ModelProperty(dataModel.SettingsModel())

  private var userData: Option[UserContextData] = None

  properties.subProp(_.token).listen {token =>
    val ctx = new UserContextData(token, rpc)
    ctx.api.user.foreach { u =>
      println(s"Login - new user ${u.login}:${u.name}")
      properties.subProp(_.user).set(UserLoginModel(u.login, u.name))
    }
    userData = Some(ctx)
  }

  def logout(): Future[Unit] = {
    Future.failed(new UnsupportedOperationException)
  }

  def user: Option[Future[User]] = userData.map(_.api.user)

  def loadIssues(): Future[LoadedActivities] = {
    userData.map(_.loadCached()).getOrElse(Future.failed(new NoSuchElementException()))
  }
}
