package com.github.opengrabeso.cohabo
package frontend
package views
package settings

import com.github.opengrabeso.cohabo.frontend.dataModel.SettingsModel

import java.time.{ZoneId, ZonedDateTime}
import common.model._
import common.css._
import io.udash._
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.form.{UdashForm, UdashInputGroup}
import io.udash.component.ComponentId
import io.udash.css._
import org.scalajs.dom


class PageView(
  model: ModelProperty[PageModel],
  presenter: PagePresenter,
) extends View with CssView with PageUtils with TimeFormatting {
  val s = SelectPageStyles

  import scalatags.JsDom.all._

  private val submitButton = UdashButton(componentId = ComponentId("about"))(_ => "Submit")

  buttonOnClick(submitButton){presenter.submit()}

  def template(model: ModelProperty[SettingsModel], presenter: PagePresenter): dom.Element = {
    div(
      UdashForm(inputValidationTrigger = UdashForm.ValidationTrigger.OnChange)(factory => Seq[Modifier](
        factory.input.formGroup()(
          input = _ => factory.input.textInput(model.subProp(_.token))().render,
          labelContent = Some(_ => "Token": Modifier),
          helpText = Some(_ => "GitHub access token": Modifier)
        )
      ))
    ).render
  }

  def getTemplate: Modifier = {

    div(
      s.settingsContainer,
      s.limitWidth,
      template(model.subModel(_.s), presenter),
      submitButton
    )
  }
}
