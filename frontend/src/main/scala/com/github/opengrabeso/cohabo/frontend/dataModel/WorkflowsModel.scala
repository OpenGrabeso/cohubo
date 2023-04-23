package com.github.opengrabeso.cohabo.frontend.dataModel

import com.avsystem.commons.serialization.json.{JsonStringInput, JsonStringOutput}
import io.udash.HasModelPropertyCreator
import org.scalajs.dom

case class WorkflowsModel(
                          token: String = null,
                          contexts: Seq[ContextModel] = Seq.empty,
                          selectedContext: Option[ContextModel] = None,
                          user: UserLoginModel = UserLoginModel(),
                          rateLimits: Option[(Long, Long, Long)] = None // limit, remaining, reset
                        ) {
  def activeContexts: Seq[ContextModel] = selectedContext.orElse(contexts.headOption).toSeq
}

object WorkflowsModel extends HasModelPropertyCreator[WorkflowsModel] {
  import ContextModel._
  def loadContexts(s: String): Seq[ContextModel] = JsonStringInput.read[Seq[ContextModel]](s)
  def saveContexts(c: Seq[ContextModel]): String = JsonStringOutput.write(c)

  private val ls = dom.window.localStorage
  private val ss = dom.window.sessionStorage
  private val values = Map[String, (WorkflowsModel => String, (WorkflowsModel, String) => WorkflowsModel)](
    "cohubo.token" -> (_.token, (m, s) => m.copy(token = s)),
    "cohubo.contexts" -> (s => saveContexts(s.contexts), (m, s) => m.copy(contexts = loadContexts(s))),
    "cohubo.selectedContext" -> (
      s => JsonStringOutput.write(s.selectedContext),
      (m, s) => m.copy(selectedContext = JsonStringInput.read[Option[ContextModel]](s))
    )
  )

  def load: WorkflowsModel = {
    values.foldLeft(WorkflowsModel()) { case (model, (k, v)) =>
      val loaded = Option(ss.getItem(k)).orElse(Option(ls.getItem(k)))
      loaded.map(s => v._2(model, s)).getOrElse(model)
    }.pipe { s =>
      if (s.selectedContext.nonEmpty) s
      else s.copy(selectedContext = s.contexts.headOption)
    }
  }

  def store(model: WorkflowsModel): Unit = {
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
