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
import io.udash.component.ComponentId
import io.udash.css._
import org.scalajs.dom
import io.udash.bootstrap.utils.BootstrapStyles._

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
      div(
        cls := "col",
        s.gridAreaNavigation,
        settingsButton.render,
        Spacing.margin(size = SpacingSize.Small),
        repoTableRender(repoTable),
        div(cls := "row justify-content-left")(addRepoButton),
      ),
      div(
        //display.none,
        addRepoModal,
      )
    ).render
  }
}
