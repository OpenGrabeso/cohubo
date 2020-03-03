package com.github.opengrabeso.cohabo
package frontend.dataModel

import common.model._
import io.udash.HasModelPropertyCreator

case class ArticleIdModel(id: String, parent: Option[String]) {
  override def toString = if (parent.isDefined) {
    s"$id:${parent.getOrElse("")}"
  } else {
    id
  }
}

object ArticleIdModel extends HasModelPropertyCreator[ArticleIdModel]


