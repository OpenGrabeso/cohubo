package com.github.opengrabeso.cohabo
package frontend
package views
package select

import io.udash._
import common.model._
import io.udash.utils.FileUploader.FileUploadModel

/** The form's model structure. */
case class PageModel(
  loading: Boolean, articles: Seq[ArticleRow], articleContent: String = "", error: Option[Throwable] = None
)
object PageModel extends HasModelPropertyCreator[PageModel]
