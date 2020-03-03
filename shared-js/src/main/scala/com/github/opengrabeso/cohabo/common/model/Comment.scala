package com.github.opengrabeso.cohabo
package common.model

import java.time.ZonedDateTime

import rest.EnhancedRestDataCompanion

case class Comment(
  url: String,
  html_url: String,
  body: String,
  user: User,
  created_at: String,
  updated_at: String
)

object Comment extends EnhancedRestDataCompanion[Comment]