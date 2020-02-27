package com.github.opengrabeso.cohabo
package frontend.model

import common.model._
import io.udash.HasModelPropertyCreator

case class Article(
  title: String, content: String, attributes: Map[String, String]
)

object Article extends HasModelPropertyCreator[Article]
