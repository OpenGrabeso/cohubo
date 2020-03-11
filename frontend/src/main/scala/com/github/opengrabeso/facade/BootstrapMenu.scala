package com.github.opengrabeso.facade

import scala.scalajs.js
import scala.scalajs.js.annotation._

import io.udash.wrappers.jquery.JQuery

// facade for https://github.com/dgoguerra/bootstrap-menu/
object BootstrapMenu extends {
  trait MenuItem[T] extends js.Object {
    def name(x: T): String
    def onClick(x: T): Unit
  }

  object MenuItem {
    def apply[T](namePar: String, onClickPar: T => Unit): MenuItem[T] = {
      new MenuItem[T] {
        override def name(x: T) = namePar
        override def onClick(x: T) = onClickPar(x)
      }
    }
    def par[T](namePar: T => String, onClickPar: T => Unit): MenuItem[T] = {
      new MenuItem[T] {
        override def name(x: T) = namePar(x)
        override def onClick(x: T) = onClickPar(x)
      }
    }
  }

  @js.native
  @JSGlobal("BootstrapMenu")
  class BootstrapMenu(selector: String, options: js.Object) extends js.Object

  trait Options[T] extends js.Object {
    def fetchElementData(e: JQuery): T
    def actions: js.Array[MenuItem[T]]
  }
}
