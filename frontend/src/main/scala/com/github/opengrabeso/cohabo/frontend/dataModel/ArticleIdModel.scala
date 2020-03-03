package com.github.opengrabeso.cohabo
package frontend.dataModel

import common.model._
import io.udash.HasModelPropertyCreator

case class ArticleIdModel(id: Long, issueNumber: Long) {
  override def toString = s"$issueNumber($id)"
}

object ArticleIdModel extends HasModelPropertyCreator[ArticleIdModel]


