package com.github.opengrabeso.cohabo
package frontend
package views
package settings_base

import java.time.ZonedDateTime

import io.udash._

case class SettingsModel(loading: Boolean = true, settings: SettingsStorage = SettingsStorage(), currentTime: ZonedDateTime = ZonedDateTime.now())

object SettingsModel extends HasModelPropertyCreator[SettingsModel]
