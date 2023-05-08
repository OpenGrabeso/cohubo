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
import io.udash.wrappers.jquery.{JQuery, jQ}
import com.github.opengrabeso.facade

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

    val repoTable = repoTableTemplate
    val stats = PagePresenter.fromRuns(model)

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
          s.optionsRow,
          RadioButtons[String](model.subProp(_.timespan), options = Seq("7 days", "30 days").toSeqProperty)(RadioButtons.spanWithLabelDecorator(x => x)),
        ),
        div(
          s.filterExpression,
          inputs.TextInput(model.subProp(_.filterExpression), 1.second)(s.filterExpressionInput)
        ).render,
      ),

      div(
        s.gridAreaTableContainer,
        div (
          s.runsTableContainer,
          UdashTable(stats.transformToSeq(_.stats))(
            headerFactory = Some { interceptor =>
              tr(th(s.th, "Name") +: PagePresenter.StatsRow().productElementNames.toSeq.map { elementName =>
                th(s.th, elementName.replaceAll("_", " "))
              })
            },
            rowFactory = (stat, interceptor) => tr(
              td(bind(stat.transform(_._1))),
              produce(stat.transformToSeq(_._2.productIterator.toSeq))(x => x.map { value =>
                val string = value match {
                  case x: Double =>
                    f"$x%.1f"
                  case x =>
                    x.toString
                }
                td(string).render
              })
            ).render
          ).render
        ),
        p(
          "Total workflows ", bind(stats.transform(_.count)),
          " Oldest workflow ", bind(stats.transform(_.oldest)),
          bind(model.subProp(_.loading).transform(b => if (b) " Loading..." else "")),
          produce(globals.subProp(_.rateLimits)) {
            case Some((limit, remaining, reset)) =>
              val now = System.currentTimeMillis() / 1000
              s" Remaining $remaining of $limit, reset in ${(reset - now) / 60} min".render
            case None =>
              "".render
          }
        ),
      ),
      div(
        //display.none,
        addRepoModal,
      )
    ).render
  }
}
