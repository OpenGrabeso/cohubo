package com.github.opengrabeso.cohabo
package frontend
package views
package workflows

import com.github.opengrabeso.cohabo.frontend.dataModel.WorkflowsModel
import io.udash._

case class PageModel(s: WorkflowsModel)

object PageModel extends HasModelPropertyCreator[PageModel]
