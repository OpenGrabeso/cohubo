package com.github.opengrabeso.cohabo.frontend.views

object Icons {
  def icon(name: String, size: Int = 16) = s"<img src='$name.svg' height=$size'/>"
  def check(size: Int = 16): String = icon("check")
}
