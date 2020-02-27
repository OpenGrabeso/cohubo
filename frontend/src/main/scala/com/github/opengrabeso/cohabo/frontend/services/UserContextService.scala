package com.github.opengrabeso.cohabo
package frontend
package services

import java.time.{ZoneOffset, ZonedDateTime}

import common.model._
import common.Util._

import scala.concurrent.{ExecutionContext, Future}
import UserContextService._
import com.github.opengrabeso.cohabo.frontend.model._
import org.scalajs.dom

object UserContextService {
  final val normalCount = 15

  case class LoadedActivities(staged: Seq[ArticleId])

  class UserContextData(userId: String, val sessionId: String, authCode: String, rpc: rest.RestAPI)(implicit ec: ExecutionContext) {
    var loaded = Option.empty[(Boolean, Future[LoadedActivities])]
    var context = UserContext(userId, authCode)

    def userAPI: rest.UserRestAPI = rpc.userAPI(context.userId, context.authCode, sessionId)

    private def doLoadActivities(): Future[LoadedActivities] = {

      Future.successful {
        LoadedActivities(
          for (i <- 1 to 10) yield ArticleId(i.toString)
        )
      }
    }

    def loadCached(): Future[LoadedActivities] = {
      if (loaded.isEmpty) {
        doLoadActivities()
      } else {
        loaded.get._2
      }
    }
  }
}

class UserContextService(rpc: rest.RestAPI)(implicit ec: ExecutionContext) {

  private var userData: Option[UserContextData] = None

  def login(userId: String, authCode: String, sessionId: String): UserContext = {
    println(s"Login user $userId session $sessionId")
    val ctx = new UserContextData(userId, sessionId, authCode, rpc)
    userData = Some(ctx)
    ctx.context
  }
  def logout(): Future[UserContext] = {
    userData.flatMap { ctx =>
      api.map(_.logout.map(_ => ctx.context))
    }.getOrElse(Future.failed(new UnsupportedOperationException))
  }

  def userName: Option[Future[String]] = api.map(_.name)
  def userId: Option[String] = userData.map(_.context.userId)

  def loadCached(): Future[LoadedActivities] = {
    userData.get.loadCached()
  }

  def api: Option[rest.UserRestAPI] = userData.map { data =>
    //println(s"Call userAPI user ${data.context.userId} session ${data.sessionId}")
    rpc.userAPI(data.context.userId, data.context.authCode, data.sessionId)
  }
}
