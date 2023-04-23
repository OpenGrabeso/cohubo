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
import io.udash.bootstrap.table.UdashTable
import org.scalajs.dom.Element

import java.time.temporal.ChronoUnit

class PageView(
  model: ModelProperty[PageModel],
  val presenter: PagePresenter,
  val globals: ModelProperty[SettingsModel]
) extends View with CssView with PageUtils with TimeFormatting with repository_base.RepoView {

  def selectedContextModel: Property[Option[ContextModel]] = model.subProp(_.selectedContext)

  val s = SelectPageStyles

  import scalatags.JsDom.all._

  def getTemplate: Modifier = {

    type DisplayAttrib = TableFactory.TableAttrib[RunModel]
    val attribs = Seq[DisplayAttrib](
      TableFactory.TableAttrib("#", (run, _, _) =>
        div(
          a(run.runId.toString, href := run.html_url),
        )
      ),
      TableFactory.TableAttrib("Workflow", (run, _, _) => run.name),
      TableFactory.TableAttrib("Branch", (run, _, _) => "???"),
      TableFactory.TableAttrib("Duration", (run, _, _) =>
        ChronoUnit.SECONDS.between(run.created_at, run.updated_at)
      ),
    )

    implicit object rowHandler extends views.TableFactory.TableRowHandler[RunModel, RunIdModel] {
      override def id(item: RunModel) = RunIdModel(item.runId)

      override def indent(item: RunModel) = 0

      override def rowModifier(itemModel: ModelProperty[RunModel]) = {
        val id = itemModel.get
        Seq[Modifier]()
      }

      def tdModifier: Modifier = s.tdRepo

      def rowModifyElement(element: Element): Unit = ()

    }

    val table = UdashTable(model.subSeq(_.runs), bordered = true.toProperty, hover = true.toProperty, small = true.toProperty)(
      headerFactory = Some(TableFactory.headerFactory(attribs)),
      rowFactory = TableFactory.rowFactory[RunModel, RunIdModel](
        false.toProperty,
        model.subProp(_.selectedRunId),
        attribs
      )
    )

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
        table.render
      ),
      div(
        //display.none,
        addRepoModal,
      )
    ).render
  }
}
