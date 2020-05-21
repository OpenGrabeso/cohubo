package com.github.opengrabeso.cohabo.frontend.views

import io.udash.wrappers.jquery.JQueryEvent

import scala.scalajs.js

trait JQEvents {
  implicit class EventOps(ev: JQueryEvent) {
    private def dyn = ev.asInstanceOf[js.Dynamic]

    def ctrlKey = dyn.ctrlKey.asInstanceOf[Boolean]
    def key = dyn.key.asInstanceOf[String]
  }
}
