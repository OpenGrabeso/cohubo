package com.github.opengrabeso.cohabo
package frontend
package views
package settings

import routing._
import io.udash._
import org.scalajs.dom

import scala.concurrent.ExecutionContext

/** Contains the business logic of this view. */
class PagePresenter(
  model: ModelProperty[PageModel],
  userContextService: services.UserContextService,
  application: Application[RoutingState]
)(implicit ec: ExecutionContext) extends Presenter[SettingsPageState.type] with settings_base.SettingsPresenter {

  val subModel = model.subModel(_.s)
  load(subModel)

  /** We don't need any initialization, so it's empty. */
  override def handleState(state: SettingsPageState.type): Unit = {
  }

  def submit(): Unit = {
    store(subModel)
    // setting the token will initiate the login
    println("Set userContextService.properties token")
    userContextService.properties.subProp(_.token).set(subModel.subProp(_.token).get)
    application.goTo(SelectPageState)
  }
}
