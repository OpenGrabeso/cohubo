package com.github.opengrabeso.cohabo
package frontend
package views.select

import routing._
import io.udash._

/**
 * Use the same factory for all views
 * see https://guide.udash.io/frontend/routing/pizza and https://guide.udash.io/frontend/routing:
 *
 * if it returns a different ViewFactory, new presenter and view will be created and rendered.
 * If the matching returns equal (value, not reference comparison) ViewFactory, then the previously created presenter
 * will be informed about the state changed through calling the handleState method.
 * */

case object PageViewFactory extends ViewFactory[SelectPageState] {

  val application = ApplicationContext.application
  val userService = ApplicationContext.userContextService

  import scala.concurrent.ExecutionContext.Implicits.global

  override def create(): (View, Presenter[SelectPageState]) = {
    val model = ModelProperty(PageModel(loading = true))

    val presenter = new PagePresenter(model, application, userService)

    val view = new PageView(model, presenter, userService.properties)

    presenter.init()

    (view, presenter)
  }
}