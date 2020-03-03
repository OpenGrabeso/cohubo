package com.github.opengrabeso.cohabo
package frontend.dataModel

import common.model._
import io.udash.HasModelPropertyCreator

case class ArticleIdModel(owner: String, repo: String, issueNumber: Long, id: Option[(Int, Long)]) {
  override def toString =
    id.map { case (index, _) =>
      s"#$issueNumber($index)"
    }.getOrElse {
      s"#$issueNumber($id)"
    }
}

object ArticleIdModel extends HasModelPropertyCreator[ArticleIdModel]


