package com.github.opengrabeso.cohabo
package frontend
package views
package settings

import com.github.opengrabeso.cohabo.frontend.dataModel.SettingsModel
import io.udash._

case class PageModel(s: SettingsModel)

object PageModel extends HasModelPropertyCreator[PageModel]
