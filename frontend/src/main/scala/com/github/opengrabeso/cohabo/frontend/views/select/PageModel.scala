package com.github.opengrabeso.cohabo
package frontend
package views
package select

import com.github.opengrabeso.cohabo.frontend.dataModel._
import io.udash._

/** The form's model structure. */
case class PageModel(
  loading: Boolean,
  repoError: Boolean = false,
  articles: Seq[ArticleRowModel] = Seq.empty,
  selectedArticleId: Option[ArticleIdModel] = None,
  selectedArticleParent: Option[ArticleRowModel] = None,
  selectedArticle: Option[ArticleRowModel] = None,
  articleContent: String = "",
  error: Option[Throwable] = None
)

object PageModel extends HasModelPropertyCreator[PageModel]
