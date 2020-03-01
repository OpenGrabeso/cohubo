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
import io.udash.wrappers.jquery.jQ

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
    val attribs = Seq[DisplayAttrib](
      TableFactory.TableAttrib("Id", (ar, _, _) => ar.id.toString.render),
      TableFactory.TableAttrib("Parent", (ar, _, _) => ar.parentId.map(_.toString).getOrElse("").render),
      TableFactory.TableAttrib("Title", (ar, _, _) => ar.title.render),
      TableFactory.TableAttrib("Posted by", (ar, _, _) => "???".render),
      TableFactory.TableAttrib("Date", (ar, _, _) => "???".render),
    )

    val table = UdashTable(model.subSeq(_.articles), bordered = true.toProperty, hover = true.toProperty, small = true.toProperty)(
      headerFactory = Some(TableFactory.headerFactory(attribs)),
      rowFactory = TableFactory.rowFactory[ArticleRowModel, ArticleIdModel](_.id, model.subProp(_.selectedArticleId), attribs)
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
              table.render
            ).render.tap(d =>
              jQ(d).find("th").asInstanceOf[js.Dynamic].resizable()
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