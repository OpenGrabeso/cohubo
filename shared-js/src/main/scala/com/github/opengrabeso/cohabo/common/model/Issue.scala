package com.github.opengrabeso.cohabo
package common.model

import java.time.ZonedDateTime

import rest.EnhancedRestDataCompanion

case class Issue(
  id: Long,
  number: Long,
  title: String,
  body: String,
  user: User,
  comments: Long,
  created_at: ZonedDateTime,
  updated_at: ZonedDateTime
)

object Issue extends EnhancedRestDataCompanion[Issue]
