package com.github.opengrabeso.cohabo

import io.udash.properties.ModelPropertyCreator
import io.udash.rest.RestDataCompanion

@SerialVersionUID(12)
case class SettingsStorage(questTimeOffset: Int = 0, maxHR: Int = 220, elevFilter: Int = 2) {
  def setQuestTimeOffset(v: Option[Int]) = v.map(v => copy(questTimeOffset = v)).getOrElse(this)
  def setMaxHR(v: Option[Int]) = v.map(v => copy(maxHR = v)).getOrElse(this)
  def setElevFilter(v: Option[Int]) = v.map(v => copy(elevFilter = v)).getOrElse(this)
}

object SettingsStorage extends RestDataCompanion[SettingsStorage] {
  implicit val modelPropertyCreator = ModelPropertyCreator.materialize[SettingsStorage]
}
