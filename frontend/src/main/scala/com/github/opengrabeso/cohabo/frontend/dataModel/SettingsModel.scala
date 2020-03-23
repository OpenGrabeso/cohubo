package com.github.opengrabeso.cohabo.frontend.dataModel

import io.udash.HasModelPropertyCreator
import org.scalajs.dom

case class SettingsModel(
  token: String = null, context: ContextModel = ContextModel(),
  user: UserLoginModel = UserLoginModel(),
  rateLimits: Option[(Long, Long, Long)] = None
)

object SettingsModel extends HasModelPropertyCreator[SettingsModel] {
  val ls = dom.window.localStorage
  val ss = dom.window.sessionStorage
  val values = Map[String, (SettingsModel => String, (SettingsModel, String) => SettingsModel)](
    "cohubo.token" -> (_.token, (m, s) => m.copy(token = s)),
    "cohubo.organization" -> (_.context.organization, (m, s) => m.copy(context = m.context.copy(organization = s))),
    "cohubo.repository" -> (_.context.repository, (m, s) => m.copy(context = m.context.copy(repository = s)))
  )

  def load: SettingsModel = {
    values.foldLeft(SettingsModel()) { case (model, (k, v)) =>
      val loaded = Option(ss.getItem(k)).orElse(Option(ls.getItem(k)))
      loaded.map(s => v._2(model, s)).getOrElse(model)
    }
  }

  def store(model: SettingsModel): Unit = {
    for ((k, v) <- values) {
      // prefer session storage if available
      val value = v._1(model)
      if (value != null) {
        ss.setItem(k, value)
        ls.setItem(k, value)
      }
    }
  }

}
