package com.github.opengrabeso.cohabo
package common.model

import java.time.ZonedDateTime

import rest.EnhancedRestDataCompanion

@SerialVersionUID(11L)
case class ArticleId(issue: String, comment: Option[String]) {
  def serialize: String = comment.map(c => s"$issue:$c").getOrElse(s"$issue")
  def link: String = {
    val base = s"https://www.github.com/???????/$issue"
    comment.map(c => base + s"#issuecomment-$c").getOrElse(base)
  }
}

object ArticleId extends EnhancedRestDataCompanion[ArticleId] {
  def deserialize(str: String) = {
    val commentPos = str.indexOf(':')
    if (commentPos >=0) new ArticleId(
      str.take(commentPos),
      Some(str.drop(commentPos +1))
    ) else new ArticleId(str, None)
  }
}