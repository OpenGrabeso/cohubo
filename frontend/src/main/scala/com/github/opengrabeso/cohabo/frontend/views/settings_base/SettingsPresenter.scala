package com.github.opengrabeso.cohabo
package frontend
package views.settings_base

import com.github.opengrabeso.cohabo.frontend.dataModel.SettingsModel
import io.udash._
import io.udash.properties.model.ModelProperty
import org.scalajs.dom

trait SettingsPresenter {
  val ls = dom.window.localStorage
  val ss = dom.window.sessionStorage
  val values = Map[String, ModelProperty[SettingsModel] => Property[String]](
    "token" -> (_.subProp(_.token)),
    "organization" -> (_.subProp(_.organization)),
    "repository" -> (_.subProp(_.repository))
  )

  def load(model: ModelProperty[SettingsModel]): Unit = {
    for ((k, v) <- values) {
      // prefer session storage if available
      Option(ss.getItem(k)).orElse(Option(ls.getItem(k))).foreach(v(model).set(_))
    }
  }

  def store(model: ModelProperty[SettingsModel]): Unit = {
    for ((k, v) <- values) {
      // prefer session storage if available
      val value = v(model).get
      if (value != null) {
        ss.setItem(k, value)
        ls.setItem(k, value)
      }
    }
  }
}
