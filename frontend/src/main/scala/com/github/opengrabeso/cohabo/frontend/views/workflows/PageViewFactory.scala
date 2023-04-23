package com.github.opengrabeso.cohabo
package frontend
package views
package workflows

import java.time.ZonedDateTime
import routing.{RoutingState, WorkflowsPageState}
import io.udash._

import scala.annotation.nowarn
import scala.concurrent.Future

/** Prepares model, view and presenter for demo view. */
@nowarn("msg=The global execution context")
class PageViewFactory(
  application: Application[RoutingState],
  userService: services.UserContextService,
) extends ViewFactory[WorkflowsPageState.type] {
  import scala.concurrent.ExecutionContext.Implicits.global

  override def create(): (View, Presenter[WorkflowsPageState.type]) = {
    val model = ModelProperty(PageModel())

    val presenter = new PagePresenter(model, userService, application)
    val view = new PageView(model, presenter, userService.properties)
    (view, presenter)
  }
}
