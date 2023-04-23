package com.github.opengrabeso.cohabo
package frontend
package views
package workflows

import com.github.opengrabeso.cohabo.frontend.dataModel._

import java.time.{ZoneId, ZonedDateTime}
import common.model._
import common.css._
import io.udash._
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.form.{UdashForm, UdashInputGroup}
import io.udash.bootstrap.utils.BootstrapStyles.SpacingSize
import scala.concurrent.duration.{span => _, _}
import io.udash.component.ComponentId
import io.udash.css._
import org.scalajs.dom
import io.udash.bootstrap.utils.BootstrapStyles._
import io.udash.bindings.inputs

class PageView(
  model: ModelProperty[PageModel],
  val presenter: PagePresenter,
  val globals: ModelProperty[SettingsModel]
) extends View with CssView with PageUtils with TimeFormatting with repository_base.RepoView {

  def selectedContextModel: Property[Option[ContextModel]] = model.subProp(_.selectedContext)

  val s = SelectPageStyles

  import scalatags.JsDom.all._

  def getTemplate: Modifier = {

    val repoTable = repoTableTemplate

    div (
      s.container,
      div(
        cls := "col",
        s.gridAreaNavigation,
        settingsButton.render,
        Spacing.margin(size = SpacingSize.Small),
        repoTableRender(repoTable),
        div(cls := "row justify-content-left")(addRepoButton),
      ),
      div(
        s.gridAreaFilters,
        div(
          s.filterExpression,
          inputs.TextInput(model.subProp(_.filterExpression), 1.second)(s.filterExpressionInput)
        ).render,
      ),

      div(
        s.gridAreaTableContainer,
      ),
      div(
        //display.none,
        addRepoModal,
      )
    ).render
  }
}
