package com.github.opengrabeso.cohabo
package frontend
package views.edit

import model._
import common.model._
import io.udash._

case class PageModel(
  loading: Boolean,
  articleId: String, title: String = "", content: String = "",
)

object PageModel extends HasModelPropertyCreator[PageModel]
