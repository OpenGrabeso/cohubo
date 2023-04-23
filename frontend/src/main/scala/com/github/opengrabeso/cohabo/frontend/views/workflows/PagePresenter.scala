package com.github.opengrabeso.cohabo
package frontend
package views
package workflows

import dataModel._
import routing._
import io.udash._

import scala.concurrent.ExecutionContext

/** Contains the business logic of this view. */
class PagePresenter(
  model: ModelProperty[PageModel],
  userContextService: services.UserContextService,
  application: Application[RoutingState]
)(implicit ec: ExecutionContext) extends Presenter[WorkflowsPageState.type] with settings_base.SettingsPresenter {

  val subModel = model.subModel(_.s)
  subModel.set(WorkflowsModel.load)

  /** We don't need any initialization, so it's empty. */
  override def handleState(state: WorkflowsPageState.type): Unit = {
  }

  def submit(): Unit = {
    WorkflowsModel.store(subModel.get)
    userContextService.properties.subProp(_.token).set(subModel.subProp(_.token).get)
    application.goTo(SelectPageState(None))
  }
}
