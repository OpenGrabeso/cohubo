package com.github.opengrabeso.cohabo.frontend.dataModel

import java.time.ZonedDateTime

import com.github.opengrabeso.github.model._
import io.udash.HasModelPropertyCreator

case class ArticleRowModel(
  id: ArticleIdModel,
  replyNumber: Int, // user friendly comment (reply) number in the issue, zero for the main issue
  hasChildren: Boolean,
  preview: Boolean, // preview means full content (comments) is not fully loaded yet
  indent: Int,
  title: String,
  body: String,
  closed: Boolean,
  labels: Seq[Label],
  assignees: Seq[User],
  milestone: Option[String],
  createdBy: User,
  rawParent: Issue, // the parent issue - used for comments to access their highlights
  createdAt: ZonedDateTime,
  lastEditedAt: ZonedDateTime,
  updatedAt: ZonedDateTime // for an issue includes its children
) {
  override def toString = (id, createdAt, lastEditedAt, updatedAt).toString()
}

object ArticleRowModel extends HasModelPropertyCreator[ArticleRowModel]

