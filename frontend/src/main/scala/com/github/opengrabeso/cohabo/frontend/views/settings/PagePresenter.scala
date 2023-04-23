package com.github.opengrabeso.cohabo
package frontend
package views
package settings

import dataModel._
import routing._
import io.udash._

import scala.concurrent.ExecutionContext

/** Contains the business logic of this view. */
class PagePresenter(
  model: ModelProperty[PageModel],
  userContextService: services.UserContextService,
  application: Application[RoutingState]
)(implicit ec: ExecutionContext) extends Presenter[SettingsPageState.type] {

  val subModel = model.subModel(_.s)
  subModel.set(SettingsModel.load)

  /** We don't need any initialization, so it's empty. */
  override def handleState(state: SettingsPageState.type): Unit = {
  }

  def submit(): Unit = {
    SettingsModel.store(subModel.get)
    userContextService.properties.subProp(_.token).set(subModel.subProp(_.token).get)
    application.goTo(SelectPageState(None))
  }
}
