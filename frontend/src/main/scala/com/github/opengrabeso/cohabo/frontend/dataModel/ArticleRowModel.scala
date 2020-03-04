package com.github.opengrabeso.cohabo.frontend.dataModel

import java.time.ZonedDateTime

import io.udash.HasModelPropertyCreator

case class ArticleRowModel(
  id: ArticleIdModel,
  hasChildren: Boolean,
  indent: Int,
  title: String,
  body: String,
  milestone: Option[String],
  createdBy: String,
  updatedAt: ZonedDateTime
)

object ArticleRowModel extends HasModelPropertyCreator[ArticleRowModel]
