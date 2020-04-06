package com.github.opengrabeso.cohabo
package frontend.dataModel

import scalatags.JsDom
import scalatags.JsDom.all._
import io.udash.HasModelPropertyCreator
import org.scalajs.dom.html.Anchor


case class ArticleIdModel(owner: String, repo: String, issueNumber: Long, id: Option[(Int, Long)]) {

  def context = ContextModel(owner, repo)

  def sameIssue(that: ArticleIdModel) = this.owner == that.owner && this.repo == that.repo && this.issueNumber == that.issueNumber

  override def toString = {
    id.map { case (index, _) =>
      s"$owner/$repo/#$issueNumber($index)"
    }.getOrElse {
      s"$owner/$repo/#$issueNumber($id)"
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

  def issueIdName(prefix: String): String = {
    s"$prefix#$issueNumber"
  }

  def issueLink(prefix: String): JsDom.TypedTag[Anchor] = {
    a(
      href := issueUri,
      id.map(commentId => s"(${commentId._1})").getOrElse(issueIdName(prefix)).render
    )
  }
  def issueLinkFull(prefix: String): JsDom.TypedTag[Anchor] = {
    a(
      href := issueUri,
      id.map(commentId => issueIdName(prefix) + s"(${commentId._1})").getOrElse(issueIdName(prefix)).render
    )
  }
}


object ArticleIdModel extends HasModelPropertyCreator[ArticleIdModel]


