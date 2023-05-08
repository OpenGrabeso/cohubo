package com.github.opengrabeso.cohabo.frontend.dataModel

import io.udash._

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

case class RunModel(
  runId: Long,
  html_url: String,
  name: String,
  branch: String,
  run_started_at: ZonedDateTime,
  created_at: ZonedDateTime,
  updated_at: ZonedDateTime,
) {
  def duration: Long = ChronoUnit.SECONDS.between(created_at, updated_at)
}

object RunModel extends HasModelPropertyCreator[RunModel]
