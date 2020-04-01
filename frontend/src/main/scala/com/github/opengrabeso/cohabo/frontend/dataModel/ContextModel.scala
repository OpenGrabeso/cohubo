package com.github.opengrabeso.cohabo.frontend.dataModel

import io.udash.HasModelPropertyCreator
import org.scalajs.dom

case class ContextModel(organization: String = null, repository: String = null) {
  def valid = organization != null && organization.nonEmpty && repository != null && repository.nonEmpty

  def relativeUrl = organization + "/" + repository

}

object ContextModel extends HasModelPropertyCreator[ContextModel] {
  def parse(str: String): ContextModel = {
    str.split('/').toSeq match {
      case Seq(owner, repo) =>
        ContextModel(owner, repo)
    }
  }

}
