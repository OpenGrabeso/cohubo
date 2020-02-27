package com.github.opengrabeso.cohabo

import scala.scalajs.js
import js.annotation._
import io.udash.wrappers.jquery.jQ
import org.scalajs.dom

object MainJS {
  def getCookie(cookieName: String): String = {
    val allCookies = dom.document.cookie
    val cookieValue = allCookies.split(";").map(_.trim).find(_.startsWith(cookieName + "=")).map{s =>
      s.drop(cookieName.length + 1)
    }
    cookieValue.orNull
  }

  def deleteCookie(name: String): Unit = {
    // from https://www.w3schools.com/js/js_cookies.asp
    dom.document.cookie = name + "=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;"
  }

  @JSExportTopLevel("appMain") // called by index.html as an entry point
  def main(): Unit = {
    jQ((jThis: dom.Element) => {
      val appRoot = jQ("#application").get(0)
      if (appRoot.isEmpty) {
        println("Application root element not found! Check your index.html file!")
      } else {
        frontend.ApplicationContext.application.run(appRoot.get)
      }
    })
  }

}
