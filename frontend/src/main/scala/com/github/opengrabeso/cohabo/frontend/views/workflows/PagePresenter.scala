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
   val userService: services.UserContextService,
   val application: Application[RoutingState]
)(implicit ec: ExecutionContext) extends Presenter[WorkflowsPageState.type] with repository_base.RepoPresenter {

  def init(): Unit = {
    // load the settings before installing the handler
    // otherwise both handlers are called, which makes things confusing
    props.set(SettingsModel.load)

    val contexts = props.subSeq(_.contexts).get

    updateShortNames(contexts)
  }

    /** We don't need any initialization, so it's empty. */
  override def handleState(state: WorkflowsPageState.type): Unit = {
  }
}
