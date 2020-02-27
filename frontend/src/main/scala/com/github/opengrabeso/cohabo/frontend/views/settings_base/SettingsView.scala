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
          input = _ => factory.input.numberInput(model.subProp(_.settings.maxHR).transform(_.toString, _.toInt))().render,
          labelContent = Some(_ => "Max HR": Modifier),
          helpText = Some(_ => "Drop any samples with HR above this limit as erratic": Modifier)
        ),
        factory.input.formGroup()(
          input = _ => factory.input.numberInput(model.subProp(_.settings.questTimeOffset).transform(_.toString, _.toInt))().render,
          labelContent = Some(_ => "Additional sensor (e.g. Quest) time offset: ": Modifier),
          helpText = Some(_ => "Adjust so that the time below matches the time on your watches/sensor": Modifier)
        ),
        p(
          "Current time: ",
          bind(model.subProp(_.currentTime).transform(transformTime))
        ),
        p {
          val questTime = (model.subProp(_.currentTime) combine model.subProp(_.settings.questTimeOffset))(_ plusSeconds _)
          b(
            "Sensor time: ",
            bind(questTime.transform(transformTime))
          )
        },
        factory.input.formGroup()(
          input = _ => factory.input.radioButtons(
            selectedItem = model.subProp(_.settings.elevFilter),
            options = elevFilterLabel.indices.toSeqProperty,
            inline = true.toProperty,
            validationTrigger = UdashForm.ValidationTrigger.None
          )(
            labelContent = (item, _, _) => Some(label(elevFilterLabel(item)))
          ).render,
          labelContent = Some(_ => "Elevation filter:": Modifier),
          helpText = Some(_ => "Elevation data smoothing": Modifier)
        ),
      ))
    ).render

  }

}

