package com.github.opengrabeso.cohabo
package frontend
package views.edit
import common.model._
import dataModel._
import routing.{EditPageState, RoutingState}
import io.udash._

import scala.concurrent.Future

/** Prepares model, view and presenter for demo view. */
class PageViewFactory(
  application: Application[RoutingState],
  userService: services.UserContextService,
  articleId: ArticleId
) extends ViewFactory[EditPageState] {
  import scala.concurrent.ExecutionContext.Implicits.global

  override def create(): (View, Presenter[EditPageState]) = {
    val model = ModelProperty(PageModel(loading = true, articleId))


    for {
      content <- Future { // TODO: async activity get
        ArticleContent(articleId, s"Title $articleId", s"Content $articleId " * 50)
      }
    } {
      model.subProp(_.title).set(content.title)
      model.subProp(_.content).set(content.content)
      model.subProp(_.loading).set(false)
    }


    val presenter = new PagePresenter(model, userService, application)
    val view = new PageView(model, presenter)
    (view, presenter)
  }
}