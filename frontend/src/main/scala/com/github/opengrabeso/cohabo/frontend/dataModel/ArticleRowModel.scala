package com.github.opengrabeso.cohabo.frontend.dataModel

import io.udash.HasModelPropertyCreator

case class ArticleRowModel(
  id: ArticleIdModel,
  parentId: Option[ArticleIdModel],
  hasChildren: Boolean,
  indent: Int,
  title: String,
  body: String,
  createdBy: String,
  updatedAt: String
)

object ArticleRowModel extends HasModelPropertyCreator[ArticleRowModel]
