package com.github.opengrabeso.facade

import io.udash.wrappers.jquery.JQuery

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSGlobal, JSGlobalScope}

@JSGlobalScope
@js.native
object BootstrapMenu extends js.Object {
  trait MenuItem[T] extends js.Object {
    def name(x: T): String
    def onClick(x: T): Unit
  }

  @js.native
  class BootstrapMenu(selector: String, options: js.Object) extends js.Object
}
