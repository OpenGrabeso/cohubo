package com.github.opengrabeso.cohabo
package frontend.dataModel

import scalatags.JsDom
import scalatags.JsDom.all._
import io.udash.HasModelPropertyCreator
import org.scalajs.dom.html.Anchor


case class ArticleIdModel(owner: String, repo: String, issueNumber: Long, id: Option[(Int, Long)]) {
  def context = ContextModel(owner, repo)

  override def toString = {
    id.map { case (index, _) =>
      s"#$issueNumber($index)"
    }.getOrElse {
      s"#$issueNumber($id)"
    }
  }

  def from(context: ContextModel): Boolean = owner == context.organization && repo == context.repository

  def issueUri: String = {
    id.map { commentId =>
      s"https://www.github.com/$owner/$repo/issues/$issueNumber#issuecomment-${commentId._2}"
    }.getOrElse {
      s"https://www.github.com/$owner/$repo/issues/$issueNumber"
    }
  }

  def issueLink(prefix: String): JsDom.TypedTag[Anchor] = {
    a(
      href := issueUri,
      id.map(commentId => s"(${commentId._1})").getOrElse(s"$prefix#$issueNumber").render
    )
  }
  def issueLinkFull(prefix: String): JsDom.TypedTag[Anchor] = {
    a(
      href := issueUri,
      id.map(commentId => s"$prefix#$issueNumber(${commentId._1})").getOrElse(s"$prefix#$issueNumber").render
    )
  }
}


object ArticleIdModel extends HasModelPropertyCreator[ArticleIdModel]


