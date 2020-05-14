package com.github.opengrabeso.cohabo.frontend.dataModel

import java.time.ZonedDateTime

import com.github.opengrabeso.github.model._
import io.udash.HasModelPropertyCreator

case class ArticleRowModel(
  id: ArticleIdModel,
  hasChildren: Boolean,
  preview: Boolean, // preview means full content (comments) is not fully loaded yet
  indent: Int,
  title: String,
  body: String,
  closed: Boolean,
  labels: Seq[Label],
  milestone: Option[String],
  createdBy: User,
  createdAt: ZonedDateTime,
  lastEditedAt: ZonedDateTime,
  updatedAt: ZonedDateTime // for an issue includes its children
) {
  override def toString = (id, createdAt, lastEditedAt, updatedAt).toString()
}

object ArticleRowModel extends HasModelPropertyCreator[ArticleRowModel]

