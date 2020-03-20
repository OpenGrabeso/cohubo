package com.github.opengrabeso.cohabo
package common.model

import java.time.ZonedDateTime

import rest.EnhancedRestDataCompanion

// https://developer.github.com/v3/issues/comments/
case class Comment(
  id: Long,
  url: String,
  html_url: String,
  body: String,
  user: User,
  created_at: ZonedDateTime,
  updated_at: ZonedDateTime
)

object Comment extends EnhancedRestDataCompanion[Comment]