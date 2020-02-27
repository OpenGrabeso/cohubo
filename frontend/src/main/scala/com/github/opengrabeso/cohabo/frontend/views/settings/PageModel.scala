package com.github.opengrabeso.cohabo
package frontend
package views
package settings

import io.udash._

case class PageModel(s: settings_base.SettingsModel)

object PageModel extends HasModelPropertyCreator[PageModel]
