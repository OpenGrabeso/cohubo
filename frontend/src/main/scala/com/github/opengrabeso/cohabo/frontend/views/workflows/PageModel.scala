package com.github.opengrabeso.cohabo
package frontend
package views
package workflows

import dataModel._
import io.udash._

case class PageModel(
  loading: Boolean = false,
  selectedContext: Option[ContextModel] = None,
  runs: Seq[RunModel] = Seq.empty,
  selectedRunId: Option[RunIdModel] = None,
  filterExpression: String = ""
)

object PageModel extends HasModelPropertyCreator[PageModel]
