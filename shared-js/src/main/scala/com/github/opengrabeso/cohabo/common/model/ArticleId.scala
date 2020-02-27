package com.github.opengrabeso.cohabo
package common.model

import java.time.ZonedDateTime

import rest.EnhancedRestDataCompanion

@SerialVersionUID(11L)
case class ArticleId(id: String) {

  override def toString = s"$id"

  def link: String = {
    s"https://www.github.com/???????/$id"
  }
}

object ArticleId extends EnhancedRestDataCompanion[ArticleId]