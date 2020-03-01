package com.github.opengrabeso.cohabo
package frontend
package views.settings_base

import java.time.ZonedDateTime

import io.udash.properties.model.ModelProperty
import org.scalajs.dom

trait SettingsPresenter {
  def init(
    model: ModelProperty[SettingsModel], userContextService: services.UserContextService
  ): Unit = {
    model.subProp(_.settings.user).listen(p => userContextService.api.foreach(_.settings.user(p)))
    model.subProp(_.settings.organization).listen(p => userContextService.api.foreach(_.settings.organization(p)))
    model.subProp(_.settings.repository).listen(p => userContextService.api.foreach(_.settings.repository(p)))
  }
}
