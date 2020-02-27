package com.github.opengrabeso.cohabo
package frontend
package views
package edit

import model._
import routing._
import io.udash._
import common.model._
import org.scalajs.dom
import scalatags.JsDom.all._

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExportTopLevel

/** Contains the business logic of this view. */
class PagePresenter(
  model: ModelProperty[PageModel],
  userContextService: services.UserContextService,
  application: Application[RoutingState]
)(implicit ec: ExecutionContext) extends Presenter[EditPageState] {


  /** We don't need any initialization, so it's empty. */
  override def handleState(state: EditPageState): Unit = {
  }

  def edited(): Unit = {

  }
}
