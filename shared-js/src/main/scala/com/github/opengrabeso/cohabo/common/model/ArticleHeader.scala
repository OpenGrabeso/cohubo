package com.github.opengrabeso.cohabo
package common.model

import java.time.ZonedDateTime

import rest.EnhancedRestDataCompanion

@SerialVersionUID(11L)
case class ArticleHeader(id: String, name: String, createdTime: ZonedDateTime, editedTime: ZonedDateTime) {

  override def toString = s"$id: $name"

  def link: String = {
    s"https://www.github.com/???????/$id"
  }


  def shortName: String = {
    common.Formatting.shortNameString(name)
  }
}

object ArticleHeader extends EnhancedRestDataCompanion[ArticleHeader]
