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

    // time changes once per 1000 ms, but we do not know when. If one would use 1000 ms, the error could be almost 1 sec if unlucky.
    // By using 200 ms we are sure the error will be under 200 ms
    dom.window.setInterval(() => model.subProp(_.currentTime).set(ZonedDateTime.now()), 200)
  }
}
