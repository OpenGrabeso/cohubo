package com.github.opengrabeso.cohabo
package frontend.dataModel

import scalatags.JsDom
import scalatags.JsDom.all._
import io.udash.HasModelPropertyCreator
import org.scalajs.dom.html.Anchor

import scala.scalajs.js.URIUtils


case class ArticleIdModel(owner: String, repo: String, issueNumber: Long, id: Option[Long]) {

  def context = ContextModel(owner, repo)

  def sameIssue(that: ArticleIdModel) = this.owner == that.owner && this.repo == that.repo && this.issueNumber == that.issueNumber

  override def toString = {
    id.map { c =>
      s"$owner/$repo/#$issueNumber($c)"
    }.getOrElse {
      s"$owner/$repo/#$issueNumber()"
    }
  }

  def toUrlString: String = ArticleIdModel.format(this)

  def from(context: ContextModel): Boolean = owner == context.organization && repo == context.repository

  def issueUri: String = {
    id.map { commentId =>
      s"https://www.github.com/$owner/$repo/issues/$issueNumber#issuecomment-$commentId"
    }.getOrElse {
      s"https://www.github.com/$owner/$repo/issues/$issueNumber"
    }
  }

  def issueIdName(prefix: String): String = {
    s"$prefix#$issueNumber"
  }

  def issueLink(prefix: String, replyNumber: Int): JsDom.TypedTag[Anchor] = {
    a(
      href := issueUri,
      id.map(commentId => s"($replyNumber)").getOrElse(issueIdName(prefix)).render
    )
  }
  def issueLinkFull(prefix: String, replyNumber: Int): JsDom.TypedTag[Anchor] = {
    a(
      href := issueUri,
      id.map(commentId => issueIdName(prefix) + s"($replyNumber)").getOrElse(issueIdName(prefix)).render
    )
  }
}


object ArticleIdModel extends HasModelPropertyCreator[ArticleIdModel] {
  def format(id: ArticleIdModel): String = {
    val parts = Seq(id.owner, id.repo, "issues") :+ id.id.map {cid =>
      id.issueNumber.toString + "#issuecomment-" + cid.toString
    }.getOrElse {
      id.issueNumber.toString
    }
    parts.mkString("/")
  }

  def parse(s: String): Option[ArticleIdModel] = {
    val parts = s.split('/')
    parts.toSeq match {
      case Seq(owner, repo, "issues", id) =>
        // possible forms
        val PlainNum = "([0-9]+)".r // 57
        val IssueNum = "([0-9]+)#issue-([0-9]+)".r // 57#issue-590262333
        val CommentNum = "([0-9]+)#issuecomment-([0-9]+)".r // 57#issuecomment-638331665
        id match {
          case PlainNum(issueId) =>
            Some(ArticleIdModel(owner, repo, issueId.toLong, None))
          case IssueNum(issueId, _) =>
            Some(ArticleIdModel(owner, repo, issueId.toLong, None))
          case CommentNum(issueId, commentId) =>
            Some(ArticleIdModel(owner, repo, issueId.toLong, Some(commentId.toLong)))
          case _ =>
            None
        }
      case _ =>
        None
    }
  }
}


