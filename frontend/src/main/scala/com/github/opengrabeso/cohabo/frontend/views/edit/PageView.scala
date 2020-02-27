package com.github.opengrabeso.cohabo
package frontend
package views
package edit

import common.Formatting
import common.css._
import io.udash._
import io.udash.bootstrap.button.{UdashButtonGroup, UdashButtonToolbar}
import io.udash.bootstrap.form.UdashForm
import io.udash.bootstrap.table.UdashTable
import io.udash.css._
import model._
import scalatags.JsDom.all._
import io.udash.bootstrap.utils.BootstrapStyles._
import io.udash.component.ComponentId

class PageView(
  model: ModelProperty[PageModel],
  presenter: PagePresenter,
) extends FinalView with CssView with PageUtils {
  val s = EditPageStyles

  def getTemplate: Modifier = {
    div(
      s.container,

      div(
        showIfElse(model.subProp(_.loading))(
          p("Loading...").render,
          div(
            div(
              bind(model.subProp(_.title)),
              hr(),
              bind(model.subProp(_.content))
            ),
            h4("Result"),
            div(
              button(false.toProperty, "OK".toProperty).onClick(presenter.edited())
            )
          ).render
        )
      )

    )
  }
}
