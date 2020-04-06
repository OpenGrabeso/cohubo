package com.github.opengrabeso.cohabo
package frontend
package views
package select

import java.time.ZonedDateTime

import com.github.opengrabeso.facade
import common.css._
import io.udash._
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

  def shortId(context: ContextModel): String = presenter.shortRepoIds.getOrElse(context, "??")

  def fetchElementData(e: JQuery): ArticleIdModel = {
    val issueNumber = e.attr("issue-number").get.toLong
    val replyNumber = e.attr("reply-number").map(_.toInt)
    val commentNumber = e.attr("comment-number").map(_.toLong)
    val context = e.attr("issue-context").map(ContextModel.parse).get
    val commentId = (replyNumber zip commentNumber).headOption
    ArticleIdModel(context.organization, context.repository, issueNumber, commentId)
  }

  def fetchRepoData(e: JQuery): ContextModel = {
    ContextModel.parse(e.attr("repository").get)
  }

  // each row is checking dynamically in the list of unread rows using a property created by this function
  def isUnread(id: ArticleIdModel, time: ReadableProperty[ZonedDateTime]): ReadableProperty[Boolean] = {
    val key = id.context -> id.issueNumber
    model.subProp(_.unreadInfo).combine(time)(_ -> _).combine(model.subProp(_.unreadInfoFrom))(_ -> _).transform { case ((unread, time), unreadFrom ) =>
      unread.get(key).exists(_.isUnread(time)) || unreadFrom.exists(time >= _)
    }
  }

  def hasUnreadChildren(row: ReadableProperty[ArticleRowModel]): ReadableProperty[Boolean] = {
    model.subProp(_.unreadInfo).combine(row)(_ -> _).transform { case (unread, row) =>
      // when the article itself is unread, do not mark it has having unread children
      if (row.updatedAt > row.lastEditedAt) {
        val key = row.id.context -> row.id.issueNumber
        unread.get(key).exists(_.isUnread(row.updatedAt))
      } else {
        false
      }
    }

  }


  val s = SelectPageStyles

  private val settingsButton = button("Settings".toProperty)
  private val newIssueButton = button("New issue".toProperty, buttonStyle = BootstrapStyles.Color.Success.toProperty)

  private val nextPageButton = button("Load more issues".toProperty)
  private val refreshNotifications = button("Refresh notifications".toProperty)
  private val editButton = button("Edit".toProperty, model.subProp(_.editing).transform(_._1))
  private val editOKButton = button("OK".toProperty, buttonStyle = BootstrapStyles.Color.Success.toProperty)
  private val editCancelButton = button("Cancel".toProperty)

  private val addRepoButton = button("Add".toProperty, buttonStyle = BootstrapStyles.Color.Success.toProperty)

  buttonOnClick(settingsButton) {presenter.gotoSettings()}
  buttonOnClick(newIssueButton) {presenter.newIssue()}

  buttonOnClick(nextPageButton) {presenter.loadMore()}
  buttonOnClick(refreshNotifications) {presenter.refreshNotifications()}
  buttonOnClick(editButton) {presenter.editCurrentArticle()}
  buttonOnClick(editOKButton) {presenter.editOK()}
  buttonOnClick(editCancelButton) {presenter.editCancel()}

  buttonOnClick(addRepoButton) {presenter.addRepository()}

  def getTemplate: Modifier = {

    // value is a callback
    type DisplayAttrib = TableFactory.TableAttrib[ArticleRowModel]
    def widthWide(min: Int, percent: Int): Option[String] = Some(s"min-width: $min%; width $percent%")
    def width(min: Int, percent: Int, max: Int): Option[String] = Some(s"min-width: $min%; width: $percent%; max-width: $max%")

    def rowStyle(row: ModelProperty[ArticleRowModel]) = {
      // we assume id.issueNumber is not changing
      val unread = isUnread(row.get.id, row.subProp(_.lastEditedAt)).transform { b =>
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
          ar.id.issueLink(shortId(ar.id.context))
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
          CssStyleName("table-fold"),
          CssStyleName(s.tr.className),
          rowStyle(itemModel),
          CssStyleName("custom-context-menu"),
          attr("issue-context") := id.context.relativeUrl,
          attr("issue-number") := id.issueNumber,
          id.id.map(attr("reply-number") := _._1), // include only when the value is present
          id.id.map(attr("comment-number") := _._2) // include only when the value is present
        )
      }
      override def tdModifier = s.td
    }

    val table = UdashTable(model.subSeq(_.articles), bordered = true.toProperty, hover = true.toProperty, small = true.toProperty)(
      headerFactory = Some(TableFactory.headerFactory(attribs)),
      rowFactory = TableFactory.rowFactory[ArticleRowModel, ArticleIdModel](
        presenter.isEditingProperty,
        model.subProp(_.selectedArticleId),
        attribs
      )
    )

    val repoUrl = globals.subSeq(_.contexts)

    val repoAttribs = Seq[TableFactory.TableAttrib[ContextModel]](
      TableFactory.TableAttrib(
        "", { (ar, _, _) =>
          val shortName = shortId(ar)
          div(
            shortName
          ).render
        }
      ),
      TableFactory.TableAttrib(
        "Repository", { (ar, _, _) =>
        val ro = ar.relativeUrl
        div(
          ro,
          br(),
          a(
            Spacing.margin(size = SpacingSize.Small),
            href := s"https://www.github.com/$ro/issues",
            "Issues"
          ).render,
          a(
            Spacing.margin(size = SpacingSize.Small),
            href := s"https://www.github.com/$ro/milestones",
            "Milestones"
          ).render

        ).render
      } /*, style = width(5, 5, 10)*/
      ),
    )

    implicit object repoRowHandler extends views.TableFactory.TableRowHandler[ContextModel, ContextModel] {
      override def id(item: ContextModel) = item
      override def indent(item: ContextModel) = 0
      override def rowModifier(itemModel: ModelProperty[ContextModel]) = {
        val id = itemModel.get
        Seq[Modifier](
          attr("repository") := id.relativeUrl,
        )
      }
      def tdModifier: Modifier = s.tdRepo
    }

    val repoTable = UdashTable(repoUrl, bordered = true.toProperty, hover = true.toProperty, small = true.toProperty)(
      headerFactory = Some(TableFactory.headerFactory(repoAttribs)),
      rowFactory = TableFactory.rowFactory[ContextModel, ContextModel](
        false.toProperty,
        model.subProp(_.selectedContext),
        repoAttribs
      )
    )


    div(
      s.container,
      div(
        s.gridAreaNavigation,
        settingsButton.render,
        Spacing.margin(size = SpacingSize.Small),
        repoTable.render.tap { t =>
          import facade.JQueryMenu._
          import facade.Resizable._
          jQ(t).addContextMenu(
            new Options {
              override val selector = "tr"
              override val build = js.defined { (item, key) =>
                val data = fetchRepoData(item)
                val link = a(
                  href := data.absoluteUrl,
                  data.relativeUrl
                ).render.outerHTML
                new Build(
                  items = js.Dictionary(
                    "remove" -> BuildItem(s"Remove ${data.relativeUrl}", presenter.removeRepository(data)),
                    "sep2" -> "------",
                    "link" -> BuildItem(s"Copy link to $link", presenter.copyToClipboard(data.absoluteUrl), isHtmlName = true),
                    "openGitHub" -> BuildItem("Open on GitHub", presenter.gotoUrl(data.absoluteUrl)),
                  )
                )
              }
            }
          )
        },
        div(cls := "row justify-content-centwer")(
          TextInput(model.subProp(_.newRepo))(),
          addRepoButton
        ),
      ),
      div(
        s.gridAreaFilters,
        showIfElse(model.subProp(_.loading))(
          Seq.empty,
          Seq[Node](
            nextPageButton.render,
            refreshNotifications.render,
            div(s.useFlex1).render,
            newIssueButton.render,
          )
        )
      ),

      div(
        s.gridAreaTableContainer,
        //s.passFlex1,
        showIfElse(model.subProp(_.loading))(
          p("Loading...").render,
          Seq[Node](
            div(
              s.selectTableContainer,
              table.render.tap{ t =>
                import facade.Resizable._
                import facade.JQueryMenu._
                jQ(t).resizableColumns()
                jQ(t).addContextMenu(
                  new Options {
                    override val selector = ".custom-context-menu"
                    override val build = js.defined { (item, key) =>
                      val data = fetchElementData(item)
                      new Build(
                        items = js.Dictionary(
                          "markAsRead" -> BuildItem(s"Mark #${data.issueNumber} as read", presenter.markAsRead(data)),
                          "reply" -> BuildItem("Reply", presenter.reply(data)),
                          "sep2" -> "------",
                          "close" -> BuildItem("Close", presenter.closeIssue(data)),
                          "sep1" -> "------",
                          "link" -> BuildItem("Copy link to " + data.issueLinkFull(shortId(data.context)).render.outerHTML, presenter.copyLink(data), isHtmlName = true),
                          "openGitHub" -> BuildItem("Open on GitHub", presenter.gotoGithub(data)),
                        )
                      )
                    }
                  }
                )
              }
            ).render,

            div(
              s.flexRow,
              s.gridAreaTableButtons,
              // any buttons below the table may be added here - not displayed while loading
            ).render,

            div(
              s.gridAreaArticle,
              s.useFlex0,
              produce(model.subProp(_.selectedArticle).combine(model.subProp(_.selectedArticleParent))(_ -> _)) {
                case (Some(row), Some(parent)) =>
                  // if we are the issue, use our own title. If we are a comment, try extracting the title from a body
                  val title = if (row.id.id.isDefined) PagePresenter.rowTitle(row.body, parent.title) else row.title
                  div(
                    s.flexRow,
                    div(
                      s.selectedArticle,
                      h4(`class` := "title", span(title), span(`class` := "link", row.id.issueLinkFull(shortId(row.id.context)))),
                      div(span(`class` := "createdBy", row.createdBy))
                    ),
                    div(s.useFlex1),
                    div(editButton)
                  ).render
                case _ =>
                  div().render
              },
              showIfElse(model.subProp(_.editing).transform(_._1))(
                div(
                  s.editArea,
                  TextArea(model.subProp(_.editedArticleMarkdown))(Form.control, s.editTextArea, id := "edit-text-area"),
                  div(
                    s.editButtons,
                    s.flexRow,
                    div(s.useFlex1),
                    editOKButton,
                    editCancelButton
                  )
                ).render,
                div(
                  s.articleContentTextArea,
                  div(`class` := "article-content").render.tap { ac =>
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
    ).render
  }
}
