package com.github.opengrabeso.facade

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSGlobal, JSGlobalScope}

@JSGlobalScope
@js.native
object BootstrapMenu extends js.Object {
  trait MenuItem extends js.Object {
    def name: String
    def onClick(x: js.Any): Unit
  }

  @js.native
  class BootstrapMenu(selector: String, options: js.Object) extends js.Object
}
