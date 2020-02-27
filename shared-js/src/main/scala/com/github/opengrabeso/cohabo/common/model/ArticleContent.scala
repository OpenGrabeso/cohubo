package com.github.opengrabeso.cohabo
package common.model

import java.time.ZonedDateTime

import rest.EnhancedRestDataCompanion

@SerialVersionUID(11L)
case class ArticleContent(id: String, title: String, content: String) {

  def link: String = {
    s"https://www.github.com/???????/$id"
  }

  def shortName: String = {
    common.Formatting.shortNameString(title)
  }
}

object ArticleContent extends EnhancedRestDataCompanion[ArticleHeader]
