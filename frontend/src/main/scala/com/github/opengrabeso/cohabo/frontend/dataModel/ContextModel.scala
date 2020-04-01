package com.github.opengrabeso.cohabo.frontend.dataModel

import io.udash.properties.HasGenCodecAndModelPropertyCreator

case class ContextModel(organization: String = null, repository: String = null) {
  def valid = organization != null && organization.nonEmpty && repository != null && repository.nonEmpty

  def relativeUrl = organization + "/" + repository

}

object ContextModel extends HasGenCodecAndModelPropertyCreator[ContextModel] {
  def parse(str: String): ContextModel = {
    str.split('/').toSeq match {
      case Seq(owner, repo) =>
        ContextModel(owner, repo)
    }
  }

}
