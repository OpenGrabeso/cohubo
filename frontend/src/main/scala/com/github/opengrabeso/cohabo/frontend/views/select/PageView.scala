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
import org.scalajs.dom.{Element, Node}

import scala.scalajs.js
import scala.concurrent.duration.{span => _, _}
import common.Util._
import io.udash.bindings.inputs.Checkbox
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.form.UdashInputGroup

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.math.Ordered._

object PageView {
  object symbols {
    val childrenPreview = "\u2299" // (.) circled dot
    val childrenOpen = "\u02c5" // v modifier letter down
    val childrenClosed = "\u02c3" // > modifier letter right arrowhead
    val noChildren = "\u22A1" // |.| squared dot operator
    val childrenLoading = "\u2A02" // (x) circled times operator
  }
}

import PageView._

class PageView(
  model: ModelProperty[PageModel],
  presenter: PagePresenter,
  globals: ModelProperty[SettingsModel]
) extends FinalView with CssView with PageUtils with TimeFormatting with CssBase {

  def shortId(context: ContextModel): String = presenter.shortRepoIds.getOrElse(context, "??")
  def repoColor(context: ContextModel): String = {
    (shortId(context).hashCode.abs % 10).toString
  }

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

  private val filterButtons = Seq(
    div(Grid.row)(
      div(Grid.col(12, ResponsiveBreakpoint.Medium))(h4("State"))
    ),
    div(Grid.row)(
      div(Grid.col(12, ResponsiveBreakpoint.Medium))(
        UdashInputGroup()(
          UdashInputGroup.appendCheckbox(Checkbox(model.subProp(_.filterOpen))().render),
          UdashInputGroup.append(span(BootstrapStyles.InputGroup.text)("Open", s.useFlex1), s.useFlex1),
        )
      )
    ),
    div(Grid.row)(
      div(Grid.col(12, ResponsiveBreakpoint.Medium))(
        UdashInputGroup()(
          UdashInputGroup.appendCheckbox(Checkbox(model.subProp(_.filterClosed))().render),
          UdashInputGroup.append(span(BootstrapStyles.InputGroup.text)("Closed", s.useFlex1), s.useFlex1),
        )
      )
    ),
  )


  buttonOnClick(settingsButton) {presenter.gotoSettings()}
  buttonOnClick(newIssueButton) {presenter.newIssue()}

  buttonOnClick(nextPageButton) {presenter.loadMore()}
  buttonOnClick(refreshNotifications) {presenter.refreshNotifications()}
  buttonOnClick(editButton) {presenter.editCurrentArticle()}
  buttonOnClick(editOKButton) {presenter.editOK()}
  buttonOnClick(editCancelButton) {presenter.editCancel()}

  buttonOnClick(addRepoButton) {presenter.addRepository()}

  private def indentFromLevel(indent: Int): Int = {
    val base = 8
    val firstIndent = 16
    val offset = 5

    val f = (Math.log(indent + offset) - Math.log(offset)) / (Math.log(offset + 1) - Math.log(offset))
    (base + f * firstIndent).round.toInt
  }

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
      Seq(
        CssStyleName("closed").styleIf(row.get.closed),
        CssStyleName("unread").styleIf(unread),
        CssStyleName("unread-children").styleIf(hasUnreadChildren(row))
      )
    }
    val attribs = Seq[DisplayAttrib](
      TableFactory.TableAttrib("#", (ar, _, _) =>
        div(
          ar.id.id.map(_ => style := "margin-left: 20px"),
          ar.id.issueLink(shortId(ar.id.context))
        ).render,
        style = width(10, 10, 15),
        modifier = Some(ar => CssStyleName("repo-color-" + repoColor(ar.id.context)))
      ),
      //TableFactory.TableAttrib("Parent", (ar, _, _) => ar.parentId.map(_.toString).getOrElse("").render, style = width(5, 5, 10), shortName = Some("")),
      TableFactory.TableAttrib("Article Title", (ar, v, _) =>
        // unicode characters rather than FontAwesome images, as those interacted badly with sticky table header
        if (ar.hasChildren && ar.preview) {
          div(span(`class` := "preview-fold fold-open", symbols.childrenClosed), ar.title.render)
        } else if (ar.hasChildren && ar.indent > 0) {
          div(span(`class` := "fold-control fold-open", symbols.childrenOpen), ar.title.render)
        } else if (ar.hasChildren) {
          div(span(`class` := "fold-control", symbols.childrenClosed), ar.title.render)
        } else {
          div(span(`class` := "no-fold fold-open", symbols.noChildren), ar.title.render)
        },
        style = widthWide(50, 50),
        modifier = Some(ar => style := s"padding-left: ${indentFromLevel(ar.indent)}px") // item (td) style
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
      object commentLoader {
        val loading = mutable.HashMap.empty[ArticleIdModel, Future[Unit]]
      }

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
      def rowModifyElement(row: Element): Unit = {

        def openFold(tr: JQuery) = {
          // inspired from https://stackoverflow.com/a/49364929/16673

          // find all children (following items with greater level)
          def findChildren(tr: JQuery) = {
            def getDepth(d: Option[Any]) = d.map(_.asInstanceOf[Int]).getOrElse(0)
            val depth = getDepth(tr.data("depth"))
            tr.nextUntil(jQ("tr").filter((x: Element, _: Int, _: Element) => {
              getDepth(jQ(x).data("depth")) <= depth
            }))
          }

          val children = findChildren(tr)
          //println(children.length)
          val arrow = tr.find(".fold-control")
          if (children.is(":visible")) {
            arrow.html(symbols.childrenClosed)
            tr.find(".fold-control").removeClass("fold-open")
            children.hide()
          } else {
            arrow.html(symbols.childrenOpen)
            tr.find(".fold-control").addClass("fold-open")
            children.show()

            val childrenClosed = children.filter((e: Element, _: Int, _: Element) => jQ(e).find(".fold-open").length == 0)
            childrenClosed.get.foreach { close =>
              val hide = findChildren(jQ(close))
              hide.hide()
            }
          }
        }

        jQ(row).find(".fold-control").on("click", { (control, event) =>
          val tr = jQ(control).closest("tr")
          //println(tr.attr("data-depth"))
          openFold(tr)
        })

        def startLoading(tr: JQuery, wantsLoading: JQuery): Future[Unit] = {
          // first of all provide a loading feedback
          wantsLoading.removeClass("preview-fold")
          wantsLoading.text(symbols.childrenLoading)
          wantsLoading.addClass("loading-fold")

          if (true) {
            val data = fetchElementData(tr)
            commentLoader.loading.getOrElseUpdate(data, {
              val token = presenter.props.subProp(_.token).get
              val state = presenter.filterState()
              presenter.loadIssueComments(data, token, state)
            }).tap {
              // once completed, remove it from the in-progress list
              _.onComplete(_ => commentLoader.loading.remove(data))
            }
          } else {
            // create a future which will never complete
            // used for simulating long loading when debugging
            Promise[Unit].future
          }
        }

        jQ(row).on("click", { (control, event) =>
          // initiate comment loading if necessary
          val tr = jQ(control)
          val wantsLoading = tr.find(".preview-fold")
          if (wantsLoading.length > 0) {
            println(s"Clicked row with preview-fold")
            startLoading(tr, wantsLoading)
          }
        })

        jQ(row).find(".preview-fold").on("click", { (control, event) =>
          val wantsLoading = jQ(control)
          val tr = jQ(control).closest("tr")
          val rows = tr.closest("tbody")
          val data = fetchElementData(tr)
          println(s"Clicked preview-fold of $data")
          startLoading(tr, wantsLoading).onComplete { _ =>
            // beware: the corresponding row was created again when loaded, we need to find it in the rows, tr is no longer valid
            val tr = rows.find(s"[issue-number=${data.issueNumber}]")

            //println(s"Completed preview-fold of $data with $tr")

            openFold(tr)
          }
        })
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

    val repoUrl = globals.subSeq(_.contexts)

    val repoAttribs = Seq[TableFactory.TableAttrib[ContextModel]](
      TableFactory.TableAttrib(
        "", { (ar, arProp, _) =>
          val shortName = shortId(ar)
          Seq(
            Checkbox(globals.subProp(_.selectedContext).transform(_.contains(ar), b => if (b) Some(ar) else None))().render,
            " ".render,
            shortName.render
          )
        },
        modifier = Some(ar => CssStyleName("repo-color-" + repoColor(ar)))
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
      def rowModifyElement(element: Element): Unit = ()

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
        cls := "col",
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
          addRepoButton,
        ),
        filterButtons
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
                          "markAsRead" -> BuildItem(s"Mark ${data.issueIdName(shortId(data.context))} as read", presenter.markAsRead(data)),
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
            ).render.tap { d =>
              // it seems gridAreaArticle will not resize more than the initial height specified by
              // gridTemplateRows
              // we therefore keep that higher than desired and resize later runtime
              // TODO: store splitter position in localstorage / cookie / whatever
              jQ(d).height("40vh")
            },

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
