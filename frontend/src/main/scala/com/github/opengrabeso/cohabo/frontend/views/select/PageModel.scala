package com.github.opengrabeso.cohabo
package frontend
package views
package select

import java.time.ZonedDateTime

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
  pagingUrls: Map[String, String] = Map.empty,
  error: Option[Throwable] = None,
  editing: (Boolean, Boolean) = (false, false), // editing, editing is reply
  editedArticleMarkdown: String = "",
  editedArticleHTML: String = "",
  unreadInfoFrom: Option[ZonedDateTime] = None, // anything newer than the notification info must be unread
  unreadInfo: Map[Long, UnreadInfo] = Map.empty // list unread articles (and time range when unread)
)

object PageModel extends HasModelPropertyCreator[PageModel]
