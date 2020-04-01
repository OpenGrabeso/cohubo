package com.github.opengrabeso.cohabo
package frontend
package services

import common.model._
import common.Util._

import scala.concurrent.{ExecutionContext, Future, Promise}
import UserContextService._
import com.github.opengrabeso.cohabo.frontend.dataModel
import com.github.opengrabeso.cohabo.frontend.dataModel._
import com.github.opengrabeso.cohabo.rest.AuthorizedAPI
import io.udash.properties.model.ModelProperty
import org.scalajs.dom

object UserContextService {
  final val normalCount = 15

  class UserContextData(token: String, rpc: rest.RestAPI)(implicit ec: ExecutionContext) {

    def api: AuthorizedAPI = rpc.authorized("Bearer " + token)
  }
}

class UserContextService(rpc: rest.RestAPI)(implicit ec: ExecutionContext) {

  val properties = ModelProperty(dataModel.SettingsModel())

  val userData = Promise[UserContextData]

  properties.subProp(_.token).listen {token =>
    val ctx = new UserContextData(token, rpc)
    ctx.api.user.map { u =>
      println(s"Login - new user ${u.login}:${u.name}")
      properties.subProp(_.user).set(UserLoginModel(u.login, u.name))
      userData.success(ctx)
    }.failed.foreach(userData.failure)
  }

  def call[T](f: AuthorizedAPI => Future[T]): Future[T] = {
    userData.future.flatMap(d => f(d.api))
  }
}
