package com.github.opengrabeso.cohabo
package frontend
package views
package select

import common.css._
import io.udash._
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.table.UdashTable
import io.udash.bootstrap.form.UdashForm
import io.udash.css._
import scalatags.JsDom.all._
import io.udash.bootstrap._
import BootstrapStyles._
import frontend.dataModel._
import io.udash.wrappers.jquery.{JQuery, jQ}
import org.scalajs.dom.{Element, Event}

import scala.scalajs.js

class PageView(
  model: ModelProperty[PageModel],
  presenter: PagePresenter,
) extends FinalView with CssView with PageUtils with TimeFormatting {
  val s = SelectPageStyles

  private val uploadButton = UdashButton()(_ => "Upload activity data...")
  private val settingsButton = UdashButton()(_ => "Settings")

  def nothingSelected: ReadableProperty[Boolean] = model.subProp(_.selectedArticleId).transform(_.isDefined)

  private val sendToStrava = button(nothingSelected, "Send to Strava".toProperty)
  private val deleteActivity = button(nothingSelected, s"Delete from $appName".toProperty)
  private val mergeAndEdit = button(
    nothingSelected,
    "Edit...".toProperty
  )
  private val uncheckAll = button(nothingSelected, "Uncheck all".toProperty)

  buttonOnClick(settingsButton) {presenter.gotoSettings()}
  buttonOnClick(uploadButton) {presenter.uploadNewActivity()}

  def getTemplate: Modifier = {

    // value is a callback
    type DisplayAttrib = TableFactory.TableAttrib[ArticleRowModel]
    def widthWide(min: Int, percent: Int): Option[String] = Some(s"min-width: $min%; width $percent%")
    def width(min: Int, percent: Int, max: Int): Option[String] = Some(s"min-width: $min%; width: $percent%; max-width: $max%")

    val attribs = Seq[DisplayAttrib](
      TableFactory.TableAttrib("#", (ar, _, _) => Seq[Modifier](ar.id.toString.render), style = width(5, 5, 10)),
      //TableFactory.TableAttrib("Parent", (ar, _, _) => ar.parentId.map(_.toString).getOrElse("").render, style = width(5, 5, 10), shortName = Some("")),
      TableFactory.TableAttrib("Article Title", (ar, _, _) =>
        div(
          Option(i(`class`:="fold-control fas fa-angle-right rotate down")).filter(_ => ar.children.nonEmpty),
          ar.title.render,
        ).render,
        style = widthWide(50, 50),
        modifier = Some(ar => style := s"padding-left: ${ar.indent * 40}px") // item (td) style
      ),
      TableFactory.TableAttrib("Posted by", (ar, _, _) => ar.createdBy.render, style = width(10, 15, 20), shortName = Some("")),
      TableFactory.TableAttrib("Date", (ar, _, _) => ar.updatedAt.render, style = width(10, 15, 20)),
    )

    val table = UdashTable(model.subSeq(_.articles), bordered = true.toProperty, hover = true.toProperty, small = true.toProperty)(
      headerFactory = Some(TableFactory.headerFactory(attribs)),
      rowFactory = TableFactory.rowFactory[ArticleRowModel, ArticleIdModel](_.id, _.indent, model.subProp(_.selectedArticleId), attribs)
    )

    div(
      s.container,
      div(Grid.row)(
        div(Grid.col)(settingsButton.render),
      ),

      div(
        showIfElse(model.subProp(_.loading))(
          p("Loading...").render,
          div(
            bind(model.subProp(_.error).transform(_.map(ex => p(s"Error loading activities ${ex.toString}")).orNull)),
            div(
              s.selectTableContainer,
              table.render.tap { t =>
                val $ = jQ
                $(t).asInstanceOf[js.Dynamic].resizableColumns()
              }
            ),
            UdashForm()(factory => Seq[Modifier](
              factory.input.formGroup()(
                input = _ => factory.input.textArea(model.subProp(_.articleContent))(
                  Some(_ =>
                    Seq[Modifier](
                      s.articleContentTextArea
                      //rows := 12
                    )
                  )
                ).render,
                labelContent = Some(_ => "Article": Modifier)
              ).render,
            ))
          ).render

        )
      ),
      div(
        sendToStrava.render,
        mergeAndEdit.render,
        deleteActivity.render,
        uncheckAll.render
      )
    )
  }
}