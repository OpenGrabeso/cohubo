package com.github.opengrabeso.cohabo
package common.model

import java.time.ZonedDateTime

import rest.EnhancedRestDataCompanion

case class Subject(
  title: String,
  url: String,
  latest_comment_url: String,
  `type`: String
)

object Subject extends EnhancedRestDataCompanion[Subject]