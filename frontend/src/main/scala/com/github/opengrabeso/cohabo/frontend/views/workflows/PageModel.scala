package com.github.opengrabeso.cohabo
package frontend
package views
package workflows

import dataModel._
import io.udash._

case class PageModel(
  selectedContext: Option[ContextModel] = None,
  filterExpression: String = ""
)

object PageModel extends HasModelPropertyCreator[PageModel]
