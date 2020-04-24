package com.github.opengrabeso.cohabo
package frontend
package views
package settings_base

import com.github.opengrabeso.cohabo.frontend.dataModel.SettingsModel
import io.udash._
import io.udash.bootstrap.form.UdashForm
import org.scalajs.dom
import scalatags.JsDom.all._

trait SettingsView extends TimeFormatting {

  def template(model: ModelProperty[SettingsModel], presenter: SettingsPresenter): dom.Element = {
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

}

