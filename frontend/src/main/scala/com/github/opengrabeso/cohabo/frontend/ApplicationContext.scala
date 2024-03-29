package com.github.opengrabeso.cohabo
package frontend

import routing._
import common.model._
import io.udash._
import com.github.opengrabeso.github
import io.udash.rest.SttpRestClient

import scala.annotation.nowarn

@nowarn("msg=The global execution context")
object ApplicationContext {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val routingRegistry = new RoutingRegistryDef
  private val viewFactoryRegistry = new StatesToViewFactoryDef

  object githubRestApiClient extends github.RestAPIClient[github.rest.RestAPI](SttpRestClient.defaultBackend(), "https://api.github.com")

  val application = new Application[RoutingState](routingRegistry, viewFactoryRegistry)
  val userContextService = new services.UserContextService(githubRestApiClient.api)

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
      application.goTo(SelectPageState(None))
  }
}
