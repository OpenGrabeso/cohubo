package com.github.opengrabeso.cohabo.frontend.dataModel

import com.avsystem.commons.serialization.json.{JsonStringInput, JsonStringOutput}
import io.udash.HasModelPropertyCreator
import org.scalajs.dom

case class SettingsModel(
  token: String = null,
  contexts: Seq[ContextModel] = Seq.empty,
  selectedContext: Option[ContextModel] = None,
  user: UserLoginModel = UserLoginModel(),
  rateLimits: Option[(Long, Long, Long)] = None // limit, remaining, reset
) {
  def activeContexts: Seq[ContextModel] = selectedContext.orElse(contexts.headOption).toSeq
}

object SettingsModel extends HasModelPropertyCreator[SettingsModel] {
  import ContextModel._
  def loadContexts(s: String): Seq[ContextModel] = JsonStringInput.read[Seq[ContextModel]](s)
  def saveContexts(c: Seq[ContextModel]): String = JsonStringOutput.write(c)

  val ls = dom.window.localStorage
  val ss = dom.window.sessionStorage
  val values = Map[String, (SettingsModel => String, (SettingsModel, String) => SettingsModel)](
    "cohubo.token" -> (_.token, (m, s) => m.copy(token = s)),
    "cohubo.contexts" -> (s => saveContexts(s.contexts), (m, s) => m.copy(contexts = loadContexts(s))),
    "cohubo.selectedContext" -> (
      s => JsonStringOutput.write(s.selectedContext),
      (m, s) => m.copy(selectedContext = JsonStringInput.read[Option[ContextModel]](s))
    )
  )

  def load: SettingsModel = {
    values.foldLeft(SettingsModel()) { case (model, (k, v)) =>
      val loaded = Option(ss.getItem(k)).orElse(Option(ls.getItem(k)))
      loaded.map(s => v._2(model, s)).getOrElse(model)
    }.pipe { s =>
      if (s.selectedContext.nonEmpty) s
      else s.copy(selectedContext = s.contexts.headOption)
    }
  }

  def store(model: SettingsModel): Unit = {
    //println(s"Store $model")
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
