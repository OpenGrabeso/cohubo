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

object PageView {
  case class StatsRow(
    total_duration: Long = 0,
    total_count: Long = 0,
    average_duration: Long = 0
  )

  case class Stats(
    count: Int,
    oldest: Option[ZonedDateTime],
    stats: Seq[(String, StatsRow)]
  )

  def fromRuns(pageModel: ModelProperty[PageModel]): ReadableProperty[Stats] = {
    pageModel.subSeq(_.runs).transform {
      runs =>
        Stats(
          count = runs.size,
          oldest = runs.lastOption.map(_.created_at),
          stats = runs.groupBy(_.name).map { case (name, named) =>
            val total = named.map(_.duration).sum
            name -> StatsRow(
              total_duration = total,
              total_count = named.size,
              average_duration = total / named.size
            )
          }.toSeq.sortBy(-_._2.total_duration)
        )
    }
  }

}

class PageView(
  model: ModelProperty[PageModel],
  val presenter: PagePresenter,
  val globals: ModelProperty[SettingsModel]
) extends View with CssView with PageUtils with TimeFormatting with repository_base.RepoView {

  def selectedContextModel: Property[Option[ContextModel]] = model.subProp(_.selectedContext)

  val s = SelectPageStyles

  import scalatags.JsDom.all._

  private val nextPageButton = button("Load more".toProperty)

  buttonOnClick(nextPageButton) {presenter.loadMore()}

  def getTemplate: Modifier = {

    val repoTable = repoTableTemplate
    val stats = PageView.fromRuns(model)

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
              tr(th(s.th, "Name") +: PageView.StatsRow().productElementNames.toSeq.map { elementName =>
                th(s.th, elementName.replaceAll("_", " "))
              })
            },
            rowFactory = (stat, interceptor) => tr(
              td(bind(stat.transform(_._1))),
              produce(stat.transformToSeq(_._2.productIterator.toSeq))(x => x.map(value => td(value.toString).render))
            ).render
          ).render
        ),
        div(
          s.flexRow,
          nextPageButton.render,
          div(s.useFlex1).render
        ).render,
        p(
          "Total workflows ", bind(stats.transform(_.count)),
          " Oldest workflow ", bind(stats.transform(_.oldest))
        ),
      ),
      div(
        //display.none,
        addRepoModal,
      )
    ).render
  }
}
