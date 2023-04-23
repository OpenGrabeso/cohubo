package com.github.opengrabeso.cohabo.frontend.dataModel

import io.udash._

import java.time.ZonedDateTime

case class RunModel(
  runId: Long,
  html_url: String,
  name: String,
  branch: String,
  run_started_at: ZonedDateTime,
  created_at: ZonedDateTime,
  updated_at: ZonedDateTime,
)

object RunModel extends HasModelPropertyCreator[RunModel]
