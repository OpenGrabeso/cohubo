package com.github.opengrabeso.cohabo
package frontend.dataModel

import common.model._
import io.udash.HasModelPropertyCreator

case class ArticleIdModel(id: String, parent: Option[String]) {
  override def toString = s"$id:${parent.getOrElse("")}"
}

object ArticleIdModel extends HasModelPropertyCreator[ArticleIdModel]


