package com.github.opengrabeso.cohabo
package frontend

import routing._
import common.model._
import io.udash._

object ApplicationContext {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val routingRegistry = new RoutingRegistryDef
  private val viewFactoryRegistry = new StatesToViewFactoryDef

  val application = new Application[RoutingState](routingRegistry, viewFactoryRegistry)
  val userContextService = new services.UserContextService(com.github.opengrabeso.cohabo.rest.RestAPIClient.api)

  /*
  val userAPI = for {
    user <- facade.UdashApp.currentUserId
    authCode <- facade.UdashApp.currentAuthCode
  } yield {
    com.github.opengrabeso.cohabo.rest.RestAPIClient.api.userAPI(user, authCode)
  }
  */

  application.onRoutingFailure {
    case _: SharedExceptions.UnauthorizedException =>
      // automatic redirection to AboutPage
      println("A routing failure: UnauthorizedException")
      application.goTo(SelectPageState)
  }
}