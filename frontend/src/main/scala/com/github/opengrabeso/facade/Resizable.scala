package com.github.opengrabeso.facade

import io.udash.wrappers.jquery.JQuery

import scala.scalajs.js

object Resizable {
  implicit class JQueryResizableColumnsOp(jQ: JQuery) {
    def resizableColumns(): Unit = jQ.asInstanceOf[js.Dynamic].resizableColumns()
  }

}
