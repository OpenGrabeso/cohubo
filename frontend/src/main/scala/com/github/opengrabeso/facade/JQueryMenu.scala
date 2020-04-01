package com.github.opengrabeso.facade

import scala.scalajs.js
import scala.scalajs.js.annotation._
import io.udash.wrappers.jquery.JQuery

import scala.scalajs.js.|

// facade for http://swisnl.github.io/jQuery-contextMenu/

object JQueryMenu {
  // see https://swisnl.github.io/jQuery-contextMenu/docs.html
  trait Options extends js.Object {
    val selector: js.UndefOr[String] = js.undefined
    val items: js.UndefOr[Item] = js.undefined
    val trigger: js.UndefOr[String] = js.undefined
    val build: js.UndefOr[js.Function2[JQuery, js.Any, Build]] = js.undefined
  }

  class Item(
    val name: String,
    val callback: js.ThisFunction3[JQuery, Int, js.Any, js.Any, Boolean],
    val isHtmlName: js.UndefOr[Boolean] = js.undefined
  ) extends js.Object
  trait ABuildItem extends js.Object
  class BuildItem(
    val name: String,
    val callback: js.ThisFunction3[JQuery, String, js.Any, js.Any, Boolean],
    val isHtmlName: js.UndefOr[Boolean] = js.undefined
  ) extends ABuildItem

  object BuildItem {
    def apply(name: String, callback: => Unit, isHtmlName: js.UndefOr[Boolean] = js.undefined): BuildItem = {
      new BuildItem(name, (_, _, _, _) => {callback;true}, isHtmlName)
    }
  }

  class Build(
    val callback: js.UndefOr[js.ThisFunction3[JQuery, String, js.Any, js.Any, Boolean]] = js.undefined,
    val items: js.Dictionary[BuildItem | String]
  ) extends js.Object

  implicit class JQueryOp(jQ: JQuery) {
    // name contextMenu already used (for a deprecated nullary method)
    def addContextMenu(options: JQueryMenu.Options): Unit = jQ.asInstanceOf[js.Dynamic].contextMenu(options)
  }
}
