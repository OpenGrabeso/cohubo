package com.github.opengrabeso.cohabo.frontend.dataModel

import java.time.ZonedDateTime

import io.udash.HasModelPropertyCreator

case class ArticleRowModel(
  id: ArticleIdModel,
  hasChildren: Boolean,
  preview: Boolean, // preview means full content (comments) is not fully loaded yet
  indent: Int,
  title: String,
  body: String,
  milestone: Option[String],
  createdBy: String,
  createdAt: ZonedDateTime,
  lastEditedAt: ZonedDateTime,
  updatedAt: ZonedDateTime
)

object ArticleRowModel extends HasModelPropertyCreator[ArticleRowModel]

