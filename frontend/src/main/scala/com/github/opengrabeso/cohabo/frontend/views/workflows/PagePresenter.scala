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

  /** We don't need any initialization, so it's empty. */
  override def handleState(state: WorkflowsPageState.type): Unit = {
  }
}
