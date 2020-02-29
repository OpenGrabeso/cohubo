package com.github.opengrabeso.cohabo
package frontend
package views
package select

import common.model._
import common.css._
import io.udash._
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.table.UdashTable
import io.udash.bootstrap.form.UdashForm
import io.udash.css._
import scalatags.JsDom.all._
import io.udash.bootstrap._
import BootstrapStyles._
import org.scalajs.dom

class PageView(
  model: ModelProperty[PageModel],
  presenter: PagePresenter,
) extends FinalView with CssView with PageUtils with TimeFormatting {
  val s = SelectPageStyles

  private val uploadButton = UdashButton()(_ => "Upload activity data...")
  private val settingsButton = UdashButton()(_ => "Settings")

  def nothingSelected: ReadableProperty[Boolean] = {
    model.subProp(_.articles).transform(!_.exists(_.selected))
  }

  private val sendToStrava = button(nothingSelected, "Send to Strava".toProperty)
  private val deleteActivity = button(nothingSelected, s"Delete from $appName".toProperty)
  private val mergeAndEdit = button(
    nothingSelected,
    model.subProp(_.articles).transform(a => if (a.count(_.selected) > 1) "Merge and edit..." else "Edit...")
  )
  private val uncheckAll = button(nothingSelected, "Uncheck all".toProperty)

  buttonOnClick(settingsButton) {presenter.gotoSettings()}
  buttonOnClick(uploadButton) {presenter.uploadNewActivity()}

  def getTemplate: Modifier = {

    // value is a callback
    type DisplayAttrib = TableFactory.TableAttrib[ArticleRow]
    val attribs = Seq[DisplayAttrib](
      TableFactory.TableAttrib("", (ar, p, nested) =>
        div(nested(checkbox(p.subProp(_.selected)))).render
      ),
      TableFactory.TableAttrib("Id", (ar, _, _) => ar.id.render),
      TableFactory.TableAttrib("Title", (ar, _, _) => ar.title.render),
      TableFactory.TableAttrib("Posted by", (ar, _, _) => "???".render),
      TableFactory.TableAttrib("Date", (ar, _, _) => "???".render),
    )

    val table = UdashTable(model.subSeq(_.articles), bordered = true.toProperty, hover = true.toProperty, small = true.toProperty)(
      headerFactory = Some(TableFactory.headerFactory(attribs)),
      rowFactory = TableFactory.rowFactory(attribs)
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
              table.render,
              script("$('th').resizable()").render
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
                labelContent = Some(_ => "Article": Modifier),
                helpText = Some(_ => "Drop any samples with HR above this limit as erratic": Modifier)
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