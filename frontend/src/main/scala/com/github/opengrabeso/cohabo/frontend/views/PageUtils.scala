package com.github.opengrabeso.cohabo
package frontend
package views

import io.udash.bindings.inputs.{Checkbox, InputBinding}
import io.udash.bootstrap.form.UdashInputGroup
import io.udash._
import io.udash.bootstrap.button.{UdashButton, UdashButtonOptions}
import common.css._
import io.udash.bootstrap._
import BootstrapStyles._
import io.udash.css.{CssStyle, CssView}
import scalatags.JsDom.all._

trait PageUtils extends common.Formatting with CssView {
  implicit class ColorOptions(c: Color) {
    def option: UdashButtonOptions = UdashButtonOptions(c.opt)
  }

  def buttonOnClick(button: UdashButton)(callback: => Unit): UdashButton = {
    button.listen {
      case UdashButton.ButtonClickEvent(_, _) =>
        callback
    }
    button
  }

  implicit class OnClick(button: UdashButton) {
    def onClick(callback: => Unit): UdashButton = buttonOnClick(button)(callback)
  }

  def checkbox(p: Property[Boolean]): UdashInputGroup = {
    UdashInputGroup()(
      UdashInputGroup.appendCheckbox(Checkbox(p)())
    )
  }

  def button(
    buttonText: ReadableProperty[String],
    disabled: ReadableProperty[Boolean] = false.toProperty,
    buttonStyle: BootstrapStyles.Color = BootstrapStyles.Color.Secondary
  ): UdashButton = {
    UdashButton(
      disabled = disabled,
      options = buttonStyle.option
    ) { _ => Seq[Modifier](
      bind(buttonText),
      Spacing.margin(size = SpacingSize.Small)
    )}
  }

  def imageButton(disabled: ReadableProperty[Boolean], name: String, altName: String, color: Color = Color.Light): UdashButton = {
    UdashButton(disabled = disabled, options = color.option) { _ => Seq[Modifier](
      img(
        src := name,
        alt := altName,
        title := altName,
      )
    )}
  }

  def iconButton(
    altName: String,
    color: Color = Color.Light,
    active: ReadableProperty[Boolean] =  UdashBootstrap.False,
    disabled: ReadableProperty[Boolean] =  UdashBootstrap.False
  )(content: Modifier*): UdashButton = {
    UdashButton(disabled = disabled, active = active, options = color.option) { _ => Seq[Modifier](
      i(
        content,
        alt := altName,
        title := altName,
      )
    )}
  }
}
