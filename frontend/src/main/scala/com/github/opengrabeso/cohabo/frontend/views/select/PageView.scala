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
import org.scalajs.dom.{Element, Event, Node}

import scala.scalajs.js
import scala.concurrent.duration.{span => _, _}

class PageView(
  model: ModelProperty[PageModel],
  presenter: PagePresenter,
  globals: ModelProperty[SettingsModel]
) extends FinalView with CssView with PageUtils with TimeFormatting {
  val s = SelectPageStyles

  private val settingsButton = UdashButton()(_ => "Settings")
  private val nextPageButton = button(model.subProp(_.pagingUrls).transform(_.isEmpty), "Load more issues".toProperty)

  buttonOnClick(settingsButton) {presenter.gotoSettings()}
  buttonOnClick(nextPageButton) {presenter.loadMore()}

  def issueLink(id: ArticleIdModel) = {
    id.id.map { commentId =>
      a(
        href := s"https://www.github.com/${id.owner}/${id.repo}/issues/${id.issueNumber}#issuecomment-${commentId._2}",
        s"(${commentId._1})"
      )
    }.getOrElse {
      a(
        href := s"https://www.github.com/${id.owner}/${id.repo}/issues/${id.issueNumber}",
        s"#${id.issueNumber}"
      )
    }
  }

  def getTemplate: Modifier = {

    // value is a callback
    type DisplayAttrib = TableFactory.TableAttrib[ArticleRowModel]
    def widthWide(min: Int, percent: Int): Option[String] = Some(s"min-width: $min%; width $percent%")
    def width(min: Int, percent: Int, max: Int): Option[String] = Some(s"min-width: $min%; width: $percent%; max-width: $max%")

    val attribs = Seq[DisplayAttrib](
      TableFactory.TableAttrib("#", (ar, _, _) =>
        div(
          ar.id.id.map(_ => style := "margin-left: 20px"),
          issueLink(ar.id)
        ).render, style = width(5, 5, 10)
      ),
      //TableFactory.TableAttrib("Parent", (ar, _, _) => ar.parentId.map(_.toString).getOrElse("").render, style = width(5, 5, 10), shortName = Some("")),
      TableFactory.TableAttrib("Article Title", (ar, _, _) =>
        // unicode characters rather than FontAwesome images, as those interacted badly with sticky table header
        if (ar.hasChildren && ar.preview) div(span(`class`:="no-fold fold-open", "\u2299"), ar.title.render) // (.)
        else if (ar.hasChildren) div(span(`class`:="fold-control fold-open", "\u02c5"), ar.title.render) // >
        else div(span(`class`:= "no-fold fold-open", "\u22A1"), ar.title.render), // |.|
        style = widthWide(50, 50),
        modifier = Some(ar => style := s"padding-left: ${8 + ar.indent * 16}px") // item (td) style
      ),
      TableFactory.TableAttrib("Milestone", (ar, _, _) => ar.milestone.getOrElse("").render, style = width(10, 15, 20), shortName = Some("")),
      TableFactory.TableAttrib("Posted by", (ar, _, _) => ar.createdBy.render, style = width(10, 15, 20), shortName = Some("")),
      TableFactory.TableAttrib("Date", (ar, _, _) => formatDateTime(ar.updatedAt.toJSDate).render, style = width(10, 15, 20)),
    )

    val table = UdashTable(model.subSeq(_.articles), bordered = true.toProperty, hover = true.toProperty, small = true.toProperty)(
      headerFactory = Some(TableFactory.headerFactory(attribs)),
      rowFactory = TableFactory.rowFactory[ArticleRowModel, ArticleIdModel](_.id, _.indent, model.subProp(_.selectedArticleId), attribs)
    )

    val repoUrl = globals.subProp(_.organization).combine(globals.subProp(_.repository))(_ -> _)
    div(
      s.container,
      div(Grid.row)(
        Spacing.margin(size = SpacingSize.Small),
        settingsButton.render,
        TextInput(globals.subProp(_.organization), debounce = 500.millis)(),
        TextInput(globals.subProp(_.repository), debounce = 500.millis)(),
        showIfElse(model.subProp(_.repoError))(
          p("???").render,
          div(
            produce(repoUrl) { case (r, o) =>
              Seq[Node](
                a(
                  Spacing.margin(size = SpacingSize.Small),
                  href := s"https://www.github.com/$r/$o",
                  s"$r/$o"
                ).render,
                a(
                  Spacing.margin(size = SpacingSize.Small),
                  href := s"https://www.github.com/$r/$o/issues",
                  s"Issues"
                ).render,
                a(
                  Spacing.margin(size = SpacingSize.Small),
                  href := s"https://www.github.com/$r/$o/milestones",
                  s"Milestones"
                ).render

              )
            }
          ).render
        )
      ),

      div(
        s.useFlex1,
        showIfElse(model.subProp(_.loading))(
          p("Loading...").render,
          div(
            s.useFlex1,
            bind(model.subProp(_.error).transform(_.map(ex => p(s"Error loading issues ${ex.toString}")).orNull)),
            div(
              s.selectTableContainer,
              table.render.tap { t =>
                jQ(t).attr("style", "display: flex; flex-direction: column; flex: 0")
                jQ(t).asInstanceOf[js.Dynamic].resizableColumns()
              }
            ),
            hr(),
            nextPageButton,
            hr(),
            div(
              s.useFlex0,
              produce(model.subProp(_.selectedArticleParent)) {
                case Some(row) =>
                  div(s.selectedArticle,
                    h4(`class`:="title", span(row.title), span(`class`:= "link", issueLink(row.id))),
                    div(span(`class`:= "createdBy", row.createdBy))
                  ).render
                case None =>
                  div().render
              },
              div(
                s.articleContentTextArea,
                div(`class`:="article-content").render.tap { ac =>
                  model.subProp(_.articleContent).listen { content =>
                    ac.asInstanceOf[js.Dynamic].innerHTML = content
                  }
                }
              )
            )
          ).render

        )
      )
    )
  }
}