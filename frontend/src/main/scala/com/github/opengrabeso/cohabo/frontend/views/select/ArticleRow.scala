package com.github.opengrabeso.cohabo
package frontend.views.select

import common.model._
import io.udash.HasModelPropertyCreator

// first parameter (h) is Cohabo staged activity
// second parameter (a) is corresponding Strava activity ID

/**
  * uploading = true means upload is in progress or has completed (with error)
  * When uploadState is non-empty it means some error was encountered duting upload
  * */
case class ArticleRow(
  id: String, title: String, selected: Boolean, uploading: Boolean = false, uploadState: String = ""
)

object ArticleRow extends HasModelPropertyCreator[ArticleRow]
