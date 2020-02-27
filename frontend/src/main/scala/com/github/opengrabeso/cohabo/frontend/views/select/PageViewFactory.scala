package com.github.opengrabeso.cohabo
package frontend
package views.select

import routing._
import io.udash._

/** Prepares model, view and presenter for demo view. */
class PageViewFactory(
  application: Application[RoutingState],
  userService: services.UserContextService
) extends ViewFactory[SelectPageState.type] {
  import scala.concurrent.ExecutionContext.Implicits.global


  override def create(): (View, Presenter[SelectPageState.type]) = {
    val model = ModelProperty(PageModel(true, Seq()))

    val presenter = new PagePresenter(model, application, userService)
    presenter.loadActivities()
    val view = new PageView(model, presenter)
    (view, presenter)
  }
}