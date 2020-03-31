package com.github.opengrabeso.cohabo
package frontend
package views
package select

import java.time.ZonedDateTime

import com.github.opengrabeso.facade
import common.css._
import io.udash._
import io.udash.bootstrap.button._
import io.udash.bootstrap.table.UdashTable
import io.udash.css._
import scalatags.JsDom.all._
import io.udash.bootstrap._
import BootstrapStyles._
import frontend.dataModel._
import io.udash.wrappers.jquery.{JQuery, jQ}
import org.scalajs.dom.Node
import scala.scalajs.js
import scala.concurrent.duration.{span => _, _}

import common.Util._
import scala.math.Ordered._

class PageView(
  model: ModelProperty[PageModel],
  presenter: PagePresenter,
  globals: ModelProperty[SettingsModel]
) extends FinalView with CssView with PageUtils with TimeFormatting with CssBase {

  def fetchElementData(e: JQuery): ArticleIdModel = {
    val issueNumber = e.attr("issue-number").get.toLong
    val replyNumber = e.attr("reply-number").map(_.toInt)
    val commentNumber = e.attr("comment-number").map(_.toLong)
    val context = globals.subModel(_.context).get
    val commentId = (replyNumber zip commentNumber).headOption
    ArticleIdModel(context.organization, context.repository, issueNumber, commentId)
  }

  // each row is checking dynamically in the list of unread rows using a property created by this function
  def isUnread(id: Long, time: ReadableProperty[ZonedDateTime]): ReadableProperty[Boolean] = {
    model.subProp(_.unreadInfo).combine(time)(_ -> _).combine(model.subProp(_.unreadInfoFrom))(_ -> _).transform { case ((unread, time), unreadFrom ) =>
      unread.get(id).exists(_.isUnread(time)) || unreadFrom.exists(time >= _)
    }
  }

  def hasUnreadChildren(row: ReadableProperty[ArticleRowModel]): ReadableProperty[Boolean] = {
    model.subProp(_.unreadInfo).combine(row)(_ -> _).transform { case (unread, row) =>
      // when the article itself is unread, do not mark it has having unread children
      if (row.updatedAt > row.lastEditedAt) {
        unread.get(row.id.issueNumber).exists(_.isUnread(row.updatedAt))
      } else {
        false
      }
    }

  }


  val s = SelectPageStyles

  private val settingsButton = button("Settings".toProperty)
  private val newIssueButton = button("New issue".toProperty, buttonStyle = BootstrapStyles.Color.Success.toProperty)

  private val nextPageButton = button("Load more issues".toProperty, model.subProp(_.pagingUrls).transform(_.isEmpty))
  private val refreshNotifications = button("Refresh notifications".toProperty)
  private val editButton = button("Edit".toProperty, model.subProp(_.editing).transform(_._1))
  private val editOKButton = button("OK".toProperty, buttonStyle = BootstrapStyles.Color.Success.toProperty)
  private val editCancelButton = button("Cancel".toProperty)

  buttonOnClick(settingsButton) {presenter.gotoSettings()}
  buttonOnClick(newIssueButton) {presenter.newIssue()}

  buttonOnClick(nextPageButton) {presenter.loadMore()}
  buttonOnClick(refreshNotifications) {presenter.refreshNotifications()}
  buttonOnClick(editButton) {presenter.editCurrentArticle()}
  buttonOnClick(editOKButton) {presenter.editOK()}
  buttonOnClick(editCancelButton) {presenter.editCancel()}

  def getTemplate: Modifier = {

    // value is a callback
    type DisplayAttrib = TableFactory.TableAttrib[ArticleRowModel]
    def widthWide(min: Int, percent: Int): Option[String] = Some(s"min-width: $min%; width $percent%")
    def width(min: Int, percent: Int, max: Int): Option[String] = Some(s"min-width: $min%; width: $percent%; max-width: $max%")

    def rowStyle(row: ModelProperty[ArticleRowModel]) = {
      // we assume id.issueNumber is not changing
      val unread = isUnread(row.get.id.issueNumber, row.subProp(_.lastEditedAt)).transform { b =>
        // never consider unread the issue we have authored
        if (row.subProp(_.createdBy).get == globals.subProp(_.user.login).get) false
        else b
      }
      val unreadChildren = hasUnreadChildren(row)

      Seq(
        CssStyleName("unread").styleIf(unread),
        CssStyleName("unread-children").styleIf(unreadChildren)
      )
    }
    val attribs = Seq[DisplayAttrib](
      TableFactory.TableAttrib("#", (ar, _, _) =>
        div(
          ar.id.id.map(_ => style := "margin-left: 20px"),
          ar.id.issueLink
        ).render, style = width(5, 5, 10)
      ),
      //TableFactory.TableAttrib("Parent", (ar, _, _) => ar.parentId.map(_.toString).getOrElse("").render, style = width(5, 5, 10), shortName = Some("")),
      TableFactory.TableAttrib("Article Title", (ar, v, _) =>
        // unicode characters rather than FontAwesome images, as those interacted badly with sticky table header
        if (ar.hasChildren && ar.preview) div(span(`class` := "no-fold fold-open", "\u2299"), ar.title.render) // (.)
        else if (ar.hasChildren && ar.indent > 0) div(span(`class` := "fold-control fold-open", "\u02c5"), ar.title.render) // v
        else if (ar.hasChildren) div(span(`class` := "fold-control", "\u02c3"), ar.title.render) // >
        else div(span(`class` := "no-fold fold-open", "\u22A1"), ar.title.render), // |.|
        style = widthWide(50, 50),
        modifier = Some(ar => style := s"padding-left: ${8 + ar.indent * 16}px") // item (td) style
      ),
      TableFactory.TableAttrib("Milestone", (ar, _, _) => div(ar.milestone.getOrElse("").render).render, style = width(5, 10, 15), shortName = Some("")),
      TableFactory.TableAttrib("Posted by", (ar, _, _) => div(ar.createdBy).render, style = width(10, 15, 20), shortName = Some("")),
      TableFactory.TableAttrib("Created at", (ar, _, _) => div(formatDateTime(ar.createdAt.toJSDate)).render, style = width(5, 10, 15)),
      TableFactory.TableAttrib("Updated at", { (ar, _, _) =>
        if (ar.updatedAt != ar.createdAt) {
          div(formatDateTime(ar.updatedAt.toJSDate)).render
        } else {
          div().render
        }
      }, style = width(5, 10, 15)),
      //TableFactory.TableAttrib("", (ar, _, _) => div("\u22EE").render, style = width(5, 5, 5)),
    )

    implicit object rowHandler extends views.TableFactory.TableRowHandler[ArticleRowModel, ArticleIdModel] {
      override def id(item: ArticleRowModel) = item.id
      override def indent(item: ArticleRowModel) = item.indent
      override def rowModifier(itemModel: ModelProperty[ArticleRowModel]) = {
        val id = itemModel.subProp(_.id).get
        Seq[Modifier](
          rowStyle(itemModel),
          CssStyleName("custom-context-menu"),
          attr("issue-number") := id.issueNumber,
          id.id.map(attr("reply-number") := _._1), // include only when the value is present
          id.id.map(attr("comment-number") := _._2) // include only when the value is present
        )
      }
    }

    val table = UdashTable(model.subSeq(_.articles), bordered = true.toProperty, hover = true.toProperty, small = true.toProperty)(
      headerFactory = Some(TableFactory.headerFactory(attribs)),
      rowFactory = TableFactory.rowFactory[ArticleRowModel, ArticleIdModel](
        presenter.isEditingProperty,
        model.subProp(_.selectedArticleId),
        attribs
      )
    )

    val repoUrl = globals.subModel(_.context)
    div(
      s.container,
      div(
        s.gridAreaFilters,
        s.flexRow,
        Spacing.margin(size = SpacingSize.Small),
        settingsButton.render,
        TextInput(repoUrl.subProp(_.organization), debounce = 500.millis)(),
        TextInput(repoUrl.subProp(_.repository), debounce = 500.millis)(),
        showIfElse(model.subProp(_.repoError))(
          p("???").render,
          div(
            produce(repoUrl) { context =>
              val ro = context.relativeUrl
              Seq[Node](
                a(
                  Spacing.margin(size = SpacingSize.Small),
                  href := s"https://www.github.com/$ro",
                  ro
                ).render,
                a(
                  Spacing.margin(size = SpacingSize.Small),
                  href := s"https://www.github.com/$ro/issues",
                  s"Issues"
                ).render,
                a(
                  Spacing.margin(size = SpacingSize.Small),
                  href := s"https://www.github.com/$ro/milestones",
                  s"Milestones"
                ).render

              )
            }
          ).render
        ),
        div(s.useFlex1),
        newIssueButton,
      ),

      div(
        s.gridAreaTableContainer,
        //s.passFlex1,
        showIfElse(model.subProp(_.loading))(
          p("Loading...").render,
          Seq[Node](
            div(
              s.selectTableContainer,
              table.render.tap { t =>
                jQ(t).attr("style", "display: flex; flex-direction: column; flex: 0")
                jQ(t).asInstanceOf[js.Dynamic].resizableColumns()
              }
            ).render,

            div(
              s.flexRow,
              s.gridAreaTableButtons,
              nextPageButton.render,
              refreshNotifications.render
            ).render,

            div(
              s.gridAreaArticle,
              s.useFlex0,
              produce(model.subProp(_.selectedArticleParent)) {
                case Some(row) =>
                  div(
                    s.flexRow,
                    div(
                      s.selectedArticle,
                      h4(`class`:="title", span(row.title), span(`class`:= "link", row.id.issueLink)),
                      div(span(`class`:= "createdBy", row.createdBy))
                    ),
                    div(s.useFlex1),
                    div(editButton)
                  ).render
                case None =>
                  div().render
              },
              showIfElse(model.subProp(_.editing).transform(_._1))(
                div(
                  TextArea(model.subProp(_.editedArticleMarkdown))(Form.control, s.editTextArea, id := "edit-text-area"),
                  div(
                    s.flexRow,
                    div(s.useFlex1),
                    editOKButton,
                    editCancelButton
                  )
                ).render,
                div(
                  s.articleContentTextArea,
                  div(`class`:="article-content").render.tap { ac =>
                    model.subProp(_.articleContent).listen { content =>
                      ac.asInstanceOf[js.Dynamic].innerHTML = content
                    }
                  }
                ).render
              )
            ).render
          )

        )
      )
    ).render.tap { t =>
      import facade.JQueryMenu
      jQ(t).asInstanceOf[js.Dynamic].contextMenu(
        new JQueryMenu.Options {
          override val selector = ".custom-context-menu"
          override val build = js.defined { (item, key) =>
            val data = fetchElementData(item)
            new JQueryMenu.Build(
              items = js.Dictionary(
                "markAsRead" -> JQueryMenu.BuildItem(s"Mark #${data.issueNumber} as read", presenter.markAsRead(data)),
                "reply" -> JQueryMenu.BuildItem("Reply", presenter.reply(data)),
                "sep2" -> "------",
                "close" -> JQueryMenu.BuildItem("Close", presenter.closeIssue(data)),
                "sep1" -> "------",
                "link" -> JQueryMenu.BuildItem("Copy link to " + data.issueLink.render.outerHTML, presenter.copyLink(data), isHtmlName = true),
                "openGitHub" -> JQueryMenu.BuildItem("Open on GitHub", presenter.gotoGithub(data)),
              )
            )
          }
        }
      )
    }
  }
}
