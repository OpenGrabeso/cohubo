package com.github.opengrabeso.cohabo.frontend.dataModel

import io.udash.HasModelPropertyCreator

case class ArticleRowModel(
  id: ArticleIdModel,
  parentId: Option[ArticleIdModel],
  children: Seq[ArticleIdModel],
  indent: Int,
  title: String,
  createdBy: String,
  updatedAt: String
)

object ArticleRowModel extends HasModelPropertyCreator[ArticleRowModel]
