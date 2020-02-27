package com.github.opengrabeso.cohabo
package frontend
package views
package settings_base

import java.time.ZonedDateTime

import io.udash._
import io.udash.bootstrap.form.UdashForm
import org.scalajs.dom
import scalatags.JsDom.all._

trait SettingsView extends TimeFormatting {
  // TODO: use DataStreamGPS.FilterSettings
  val elevFilterLabel = Array(
    "None", "Weak", "Normal", "Strong"
  )

  def transformTime(time: ZonedDateTime): String = {
    val js = time.toJSDate
    formatTimeHMS(js)
  }

  def template(model: ModelProperty[SettingsModel], presenter: SettingsPresenter): dom.Element = {
    div(
      UdashForm(inputValidationTrigger = UdashForm.ValidationTrigger.OnChange)(factory => Seq[Modifier](
        factory.input.formGroup()(
          input = _ => factory.input.textInput(model.subProp(_.settings.user))().render,
          labelContent = Some(_ => "User": Modifier),
          helpText = Some(_ => "GitHub username": Modifier)
        ),
        factory.input.formGroup()(
          input = _ => factory.input.textInput(model.subProp(_.settings.organization))().render,
          labelContent = Some(_ => "Organization: ": Modifier),
          helpText = Some(_ => "GitHub organization": Modifier)
        ),
        factory.input.formGroup()(
          input = _ => factory.input.textInput(model.subProp(_.settings.repository))().render,
          labelContent = Some(_ => "Repository:": Modifier),
          helpText = Some(_ => "GitHub Repository": Modifier)
        ),
      ))
    ).render
  }

}

