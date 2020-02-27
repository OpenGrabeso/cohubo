package com.github.opengrabeso.cohabo
package frontend
package views
package settings

import java.time.{ZoneId, ZonedDateTime}

import common.model._
import common.css._
import io.udash._
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.form.{UdashForm, UdashInputGroup}
import io.udash.component.ComponentId
import io.udash.css._


class PageView(
  model: ModelProperty[PageModel],
  presenter: PagePresenter,
) extends FinalView with CssView with PageUtils with settings_base.SettingsView {
  val s = SelectPageStyles

  import scalatags.JsDom.all._

  private val submitButton = UdashButton(componentId = ComponentId("about"))(_ => "Submit")

  buttonOnClick(submitButton){presenter.gotoSelect()}

  def getTemplate: Modifier = {

    div(
      s.container,s.limitWidth,
      template(model.subModel(_.s), presenter),
      submitButton
    )
  }
}