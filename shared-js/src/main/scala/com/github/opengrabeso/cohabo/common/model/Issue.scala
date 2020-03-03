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
  created_at: String,
  updated_at: String
)

object Issue extends EnhancedRestDataCompanion[Issue]
