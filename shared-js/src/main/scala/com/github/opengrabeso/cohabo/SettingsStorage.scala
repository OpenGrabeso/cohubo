package com.github.opengrabeso.cohabo

import io.udash.properties.ModelPropertyCreator
import io.udash.rest.RestDataCompanion

@SerialVersionUID(12)
case class SettingsStorage(user: String = "User", organization: String = "Organization", repository: String = "Repository")

object SettingsStorage extends RestDataCompanion[SettingsStorage] {
  implicit val modelPropertyCreator = ModelPropertyCreator.materialize[SettingsStorage]
}
