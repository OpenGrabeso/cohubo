package com.github.opengrabeso.cohabo
package frontend.dataModel

import io.udash.properties.HasGenCodecAndModelPropertyCreator

case class RepoRowModel(context: ContextModel, selected: Boolean = true)

object RepoRowModel extends HasGenCodecAndModelPropertyCreator[RepoRowModel]
