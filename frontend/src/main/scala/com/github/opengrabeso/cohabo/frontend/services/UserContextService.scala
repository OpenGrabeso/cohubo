package com.github.opengrabeso.cohabo
package frontend
package services

import scala.concurrent.{ExecutionContext, Future, Promise}
import UserContextService._
import com.github.opengrabeso.cohabo.frontend.dataModel._
import com.github.opengrabeso.github.{rest => githubRest}
import io.udash.properties.model.ModelProperty

object UserContextService {
  final val normalCount = 15

  class UserContextData(token: String, rpc: githubRest.RestAPI)(implicit ec: ExecutionContext) {

    def api: githubRest.AuthorizedAPI = rpc.authorized("Bearer " + token)
  }
}

class UserContextService(rpc: githubRest.RestAPI)(implicit ec: ExecutionContext) {

  val properties = ModelProperty(dataModel.SettingsModel())

  var userData: Promise[UserContextData] = _

  println(s"Create UserContextService, token ${properties.subProp(_.token).get}")
  properties.subProp(_.token).listen {token =>
    println(s"listen: Start login $token")
    userData = Promise()
    val loginFor = userData // capture the value, in case another login starts for a different token before this one is completed
    val ctx = new UserContextData(token, rpc)
    ctx.api.user.map { u =>
      println(s"Login - new user ${u.login}:${u.name}")
      properties.subProp(_.user).set(UserLoginModel(u.login, u.name))
      loginFor.success(ctx)
    }.failed.foreach(loginFor.failure)
  }

  def call[T](f: githubRest.AuthorizedAPI => Future[T]): Future[T] = {
    userData.future.flatMap(d => f(d.api))
  }
}
