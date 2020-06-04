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
  def encode(s: String): String = URIUtils.encodeURIComponent(s)
  def decode(s: String): Option[String] = Some(URIUtils.decodeURIComponent(s))


  def format(id: ArticleIdModel): String = {
    val parts = id.id.map {cid =>
      Seq(id.owner, id.repo, id.issueNumber.toString, cid.toString)
    }.getOrElse {
      Seq(id.owner, id.repo, id.issueNumber.toString)
    }
    parts.map(encode).mkString("/")
  }

  def parse(s: String): Option[ArticleIdModel] = {
    val parts = s.split('/')
    parts.toSeq match {
      case Seq(owner, repo, id) =>
        // TODO: handle a comment number as well
        Some(ArticleIdModel(owner, repo, id.toLong, None))
      case Seq(owner, repo, id, commentId) =>
        // we do not know the answer "ordinal" number, only the comment id
        // the handleState must handle this
        Some(ArticleIdModel(owner, repo, id.toLong, Some(commentId.toLong)))
      case _ =>
        None
    }
  }
}


