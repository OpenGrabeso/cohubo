package com.github.opengrabeso.cohabo
package frontend.model

import common.model._
import io.udash.HasModelPropertyCreator

case class ArticleIdModel(id: String, title: String)

object ArticleIdModel extends HasModelPropertyCreator[ArticleIdModel]


