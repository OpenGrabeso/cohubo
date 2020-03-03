package com.github.opengrabeso.cohabo
package frontend
package views
package settings_base

import java.time.ZonedDateTime

import com.github.opengrabeso.cohabo.frontend.dataModel.SettingsModel
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
          input = _ => factory.input.textInput(model.subProp(_.token))().render,
          labelContent = Some(_ => "Token": Modifier),
          helpText = Some(_ => "GitHub access token": Modifier)
        ),
        factory.input.formGroup()(
          input = _ => factory.input.textInput(model.subProp(_.organization))().render,
          labelContent = Some(_ => "Organization: ": Modifier),
          helpText = Some(_ => "GitHub organization": Modifier)
        ),
        factory.input.formGroup()(
          input = _ => factory.input.textInput(model.subProp(_.repository))().render,
          labelContent = Some(_ => "Repository:": Modifier),
          helpText = Some(_ => "GitHub Repository": Modifier)
        ),
      ))
    ).render
  }

}

