package com.github.opengrabeso.cohabo
package frontend
package views
package settings

import routing._
import io.udash._

import scala.concurrent.ExecutionContext

/** Contains the business logic of this view. */
class PagePresenter(
  model: ModelProperty[PageModel],
  userContextService: services.UserContextService,
  application: Application[RoutingState]
)(implicit ec: ExecutionContext) extends Presenter[SettingsPageState.type] with settings_base.SettingsPresenter {

  init(model.subModel(_.s), userContextService)

  /** We don't need any initialization, so it's empty. */
  override def handleState(state: SettingsPageState.type): Unit = {
  }

  def gotoSelect(): Unit = {
    application.goTo(SelectPageState)
  }
}
