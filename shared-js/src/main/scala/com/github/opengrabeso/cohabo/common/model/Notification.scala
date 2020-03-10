package com.github.opengrabeso.cohabo
package common.model

import java.time.ZonedDateTime

import rest.EnhancedRestDataCompanion

case class Notification(
  id: Long,
  repository: Repository,
  subject: String,
  reason: String,
  unread: Boolean,
  updated_at: ZonedDateTime,
  last_read_at: ZonedDateTime,
  url: String
)

object Notification extends EnhancedRestDataCompanion[Notification]