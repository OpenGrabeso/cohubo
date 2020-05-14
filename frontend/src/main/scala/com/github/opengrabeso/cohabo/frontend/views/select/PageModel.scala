package com.github.opengrabeso.cohabo
package frontend
package views
package select

import java.time.ZonedDateTime

import com.github.opengrabeso.cohabo.frontend.dataModel._
import io.udash._
import com.github.opengrabeso.github.model._

/** The form's model structure. */
case class PageModel(
  loading: Boolean,
  articles: Seq[ArticleRowModel] = Seq.empty,
  selectedContext: Option[ContextModel] = None,
  selectedArticleId: Option[ArticleIdModel] = None,
  selectedArticleParent: Option[ArticleRowModel] = None,
  selectedArticle: Option[ArticleRowModel] = None,
  filterOpen: Boolean = true,
  filterClosed: Boolean = false,
  labels: Seq[Label] = Seq.empty,
  activeLabels: Seq[String] = Seq.empty,
  articleContent: String = "",
  editing: (Boolean, Boolean) = (false, false), // editing, editing is reply
  editedArticleMarkdown: String = "",
  editedArticleHTML: String = "",
  unreadInfoFrom: Option[ZonedDateTime] = None, // anything newer than the notification info must be unread
  unreadInfo: Map[(ContextModel, Long), UnreadInfo] = Map.empty // list unread articles (and time range when unread)
)

object PageModel extends HasModelPropertyCreator[PageModel]
