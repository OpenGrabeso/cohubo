package com.github.opengrabeso.cohabo.frontend.dataModel

import io.udash.HasModelPropertyCreator
import org.scalajs.dom

case class ContextModel(organization: String = null, repository: String = null) {
  def relativeUrl = organization + "/" + repository

}

object ContextModel extends HasModelPropertyCreator[ContextModel]
