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
import com.github.opengrabeso.github.model._
import frontend.dataModel._
import io.udash.wrappers.jquery.{JQuery, jQ}
import org.scalajs.dom.{Element, Node}

import scala.scalajs.js
import common.Util._
import io.udash.bootstrap.button.{UdashButton, UdashButtonOptions}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.{span => _, _}
import scala.math.Ordered._
import ColorUtils.{Color, _}
import com.avsystem.commons.BSeq
import io.udash.bindings.inputs
import io.udash.bootstrap.modal.UdashModal
import org.scalajs.dom

import scala.annotation.nowarn

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

@nowarn("msg=The global execution context")
class PageView(
  model: ModelProperty[PageModel],
  presenter: PagePresenter,
  globals: ModelProperty[SettingsModel]
) extends View with CssView with PageUtils with TimeFormatting with CssBase with JQEvents {
  val s = SelectPageStyles

  def shortId(context: ContextModel): String = presenter.shortRepoIds.getOrElse(context, "??")
  def repoColor(context: ContextModel): String = {
    (shortId(context).hashCode.abs % 10).toString
  }

  def userInitials(user: User): String = {
    user.login.take(1).toUpperCase ++ user.login.drop(1).filter(_.isUpper)
  }

  def avatarHtml(user: User): Node = {
    if (user.avatar_url != null && user.avatar_url.nonEmpty) img(src := user.avatar_url, s.userIcon).render
    else span(userInitials(user)).render
  }

  def userHtml(user: User): Node = {
    span(
      avatarHtml(user),
      user.displayName.render
    ).render
  }

  def userHtmlShort(user: User): Node = {
    span(
      if (user.avatar_url != null && user.avatar_url.nonEmpty) img(src := user.avatar_url, s.userIcon).render
      else "".render,
      userInitials(user)
    ).render
  }

  def progressHtml(percent: Int): Node = {
    span(
      s.progressBackground,
      span(
        s.progressForeground,
        style := s"width: $percent%"
      )
    ).render
  }


  def fetchElementData(e: JQuery): (ArticleIdModel, Int) = {
    val issueNumber = e.attr("issue-number").get.toLong
    val replyNumber = e.attr("reply-number").get.toInt
    val commentNumber = e.attr("comment-number").map(_.toLong)
    val context = e.attr("issue-context").map(ContextModel.parse).get
    ArticleIdModel(context.organization, context.repository, issueNumber, commentNumber) -> replyNumber
  }

  def fetchElementLabels(e: JQuery): String = {
    e.attr("issue-labels").get
  }
  def fetchElementAssignees(e: JQuery): String = {
    e.attr("issue-assignees").get
  }

  // searching in the quoted comma separated list for a quoted string is much easier than parsing the list
  def elementLabelsContains(list: String, l: String): Boolean = {
    list.contains("\"" + l + "\"")
  }
  def elementAssigneesContains(list: String, l: String): Boolean = {
    list.contains("\"" + l + "\"")
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

  def isSelected(id: ArticleIdModel): ReadableProperty[Boolean] = {
    model.subProp(_.selectedArticleId).transform(_.contains(id))
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



  private val settingsButton = button("Settings".toProperty)
  private val newIssueButton = button("New issue".toProperty, buttonStyle = BootstrapStyles.Color.Success)

  private val nextPageButton = button("Load more issues".toProperty)
  private val refreshNotifications = button("Refresh notifications".toProperty)

  private val editButton = button("Edit".toProperty, model.subProp(_.editing).transform(_._1))
  private val replyButton = button("Reply".toProperty,
    model.subProp(_.selectedArticleId).transform(_.isEmpty) || model.subProp(_.editing).transform(_._1)
  )
  private val editOKButton = button("OK".toProperty, buttonStyle = BootstrapStyles.Color.Success)
  private val editCancelButton = button("Cancel".toProperty)

  private val addRepoButton = button("Add repository".toProperty, buttonStyle = BootstrapStyles.Color.Success)
  private val addRepoInput = Property[String]("")

  private val addRepoOkButton = UdashButton(options = BootstrapStyles.Color.Success.option)(_ => Seq[Modifier](UdashModal.CloseButtonAttr, "OK"))
    .tap(buttonOnClick(_)(presenter.addRepository(addRepoInput.get)))

  val addRepoModal = UdashModal(Some(Size.Small).toProperty)(
    headerFactory = Some(_ => div("Add repository").render),
    bodyFactory = Some { nested =>
      div(
        Spacing.margin(),
        Card.card, Card.body, Background.color(BootstrapStyles.Color.Light),
      )(
        "User/Repository:",
        TextInput(addRepoInput)()
      ).render
    },
    footerFactory = Some { _ =>
      div(
        addRepoOkButton.render,
        UdashButton(options = BootstrapStyles.Color.Danger.option)(_ => Seq[Modifier](UdashModal.CloseButtonAttr, "Cancel")).render
      ).render
    }
  )

  def showRepoModal(): Unit = {
    addRepoInput.set("")
    addRepoModal.show()
  }

  private def createFilterHeader(content: Modifier*) = {
    div(Grid.row)(
      div(Grid.col(12, ResponsiveBreakpoint.Medium))(h4(content))
    )
  }

  private def buttonColors(b: Boolean, color: String) = {
    val bColor = Color.parseHex(color)
    val background = if (!b) bColor else bColor * 0.5
    (
      background.toHex,
      if (background.brightness >= 100) "#000000" else "#ffffff"
    )
  }

  private def createColoredButton(prop: ReadableProperty[Boolean], text: Modifier, buttonColor: String) = {
    UdashButton(options = UdashButtonOptions(size = BootstrapStyles.Size.Small.opt)) { nested =>
      Seq[Modifier](
        Spacing.margin(size = SpacingSize.ExtraSmall),
        nested(backgroundColor.bind(prop.transform(buttonColors(_, buttonColor)._1))),
        nested(color.bind(prop.transform(buttonColors(_, buttonColor)._2))),
        nested(borderWidth.bind(prop.transform(x => if (x) "4px" else "1px"))),
        nested(padding.bind(prop.transform(x => if (x) "1px 5px" else "4px 8px"))),
        text,
        s.labelButton
      )
    }
  }

  private def createColoredToggleButton(prop: Property[Boolean], text: String, buttonColor: String) = {
    createColoredButton(prop, text, buttonColor).tap { _.listen {case _ =>
      prop.set(!prop.get)
    }}
  }

  private val filterButtons = Seq(
    createFilterHeader("State"),
    div(s.labelButtons,
      createColoredToggleButton(model.subProp(_.filterOpen), "Open", "30e030"),
      createColoredToggleButton(model.subProp(_.filterClosed), "Closed", "c03030")
    )
  )

  private val labelButtons = Seq(
    createFilterHeader("Labels"),
    div(s.labelButtons,
      produceWithNested(model.subSeq(_.labels)) { (labels, nested) =>
        val selectedLabels = model.subSeq(_.activeLabels)
        labels.map { label =>
          val prop = selectedLabels.transform((s: BSeq[String]) => s.contains(label.name))
          createColoredButton(prop, label.name, label.color).tap {
            _.listen { case _ =>
              if (selectedLabels.get.contains(label.name)) {
                selectedLabels.remove(label.name)
              } else {
                selectedLabels.append(label.name)
              }
            }
          }.render
        }
      }
    )

  )

  private val assignButtons = Seq(
    createFilterHeader("Assigned to"),
    div(s.labelButtons,
      produceWithNested(model.subSeq(_.selectedContextCollaborators)) { (users, nested) =>
        users.map { user =>
          val selectedUser = model.subProp(_.filterUser)
          val prop = selectedUser.transform((s: Option[String]) => s.contains(user.login))
          createColoredButton(prop, userHtml(user), "f0f0f0").tap {
            _.listen { case _ =>
              if (selectedUser.get.contains(user.login)) {
                selectedUser.set(None)
              } else {
                selectedUser.set(Some(user.login))
              }
            }
          }.render
        }
      }
    )
  )


  buttonOnClick(settingsButton) {presenter.gotoSettings()}
  buttonOnClick(newIssueButton) {presenter.newIssue()}

  buttonOnClick(nextPageButton) {presenter.loadMore()}
  buttonOnClick(refreshNotifications) {presenter.refreshNotifications()}
  buttonOnClick(editButton) {presenter.editCurrentArticle()}
  buttonOnClick(replyButton) {presenter.reply(model.subProp(_.selectedArticleId).get.get)}
  buttonOnClick(editOKButton) {presenter.editOK()}
  buttonOnClick(editCancelButton) {presenter.editCancel()}

  buttonOnClick(addRepoButton) {showRepoModal()}

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
        if (row.subProp(_.createdBy).get.login == globals.subProp(_.user.login).get) false
        else b
      }
      val selected = isSelected(row.get.id)
      Seq(
        CssStyleName("closed").styleIf(row.get.closed),
        CssStyleName("selected").styleIf(selected),
        CssStyleName("unread").styleIf(unread),
        CssStyleName("unread-children").styleIf(hasUnreadChildren(row))
      )
    }


    def labelHtml(label: Label): Node = {
      def contrastColor(c: String) = {
        if (Color.parseHex(c).brightness >= 100) "#000000" else "#ffffff"
      }
      def nonWhiteColor(c: String) = {
        val cc = Color.parseHex(c)
        val maxBrightness = 240
        if (Color.parseHex(c).brightness > maxBrightness) (cc * (maxBrightness / 255.0)).toHex else "#" + c
      }
      def darkBorderColor(c: String) = {
        val cc = Color.parseHex(c)
        if (cc.brightness >= 100) (cc * 0.8).toHex else "#" + c
      }
      span(
        label.name,
        backgroundColor := nonWhiteColor(label.color),
        color := contrastColor(label.color),
        borderColor := darkBorderColor(label.color),
        s.labelInline
      ).render
    }

    def rowTitle(ar: ArticleRowModel): Modifier = {
      val highlights = ar.rawParent.text_matches.flatMap(_.matches.map(_.text)).distinct
      val title = Highlight(ar.title, highlights.toSeq)
      val tasks = if (ar.id.id.isEmpty) TaskList.progress(ar.body) else TaskList.Progress(0, 0)
      def seqIf(cond: Boolean)(elems: =>Seq[Modifier]): Seq[Modifier] = if (cond) elems else Seq.empty

      Seq[Modifier](raw(title)) ++
        seqIf(tasks.total > 0) {Seq(" ", span(s.taskListProgress, s"${tasks.done} of ${tasks.total}", " ", progressHtml(tasks.percent)))} ++
        seqIf(ar.labels.nonEmpty) {Seq(" ", ar.labels.map(labelHtml))} ++
        seqIf(ar.assignees.nonEmpty) {Seq(" ", ar.assignees.map(userHtmlShort))}
    }

    val attribs = Seq[DisplayAttrib](
      TableFactory.TableAttrib("#", (ar, _, _) =>
        div(
          ar.id.id.map(_ => style := "margin-left: 20px"),
          ar.id.issueLink(shortId(ar.id.context), ar.replyNumber)
        ).render,
        style = width(10, 10, 15),
        modifier = Some(ar => CssStyleName("repo-color-" + repoColor(ar.id.context)))
      ),
      //TableFactory.TableAttrib("Parent", (ar, _, _) => ar.parentId.map(_.toString).getOrElse("").render, style = width(5, 5, 10), shortName = Some("")),
      TableFactory.TableAttrib("Article Title", (ar, v, _) =>
        // unicode characters rather than FontAwesome images, as those interacted badly with sticky table header
        if (ar.hasChildren && ar.preview) {
          div(span(`class` := "preview-fold fold-open", symbols.childrenClosed))(rowTitle(ar))
        } else if (ar.hasChildren && ar.indent > 0) {
          div(span(`class` := "fold-control fold-open", symbols.childrenOpen))(rowTitle(ar))
        } else if (ar.hasChildren) {
          div(span(`class` := "fold-control", symbols.childrenClosed))(rowTitle(ar))
        } else {
          div(span(`class` := "no-fold fold-open", symbols.noChildren))(rowTitle(ar))
        },
        style = widthWide(50, 50),
        modifier = Some(ar => style := s"padding-left: ${indentFromLevel(ar.indent)}px") // item (td) style
      ),
      TableFactory.TableAttrib("Posted by", (ar, _, _) => div(userHtml(ar.createdBy)).render, style = width(10, 15, 20), shortName = Some("")),
      TableFactory.TableAttrib("Created at", (ar, _, _) => div(formatDateTime(ar.createdAt.toJSDate)).render, style = width(5, 15, 20)),
      TableFactory.TableAttrib("Updated at", { (ar, _, _) =>
        if (ar.updatedAt != ar.createdAt) {
          div(formatDateTime(ar.updatedAt.toJSDate)).render
        } else {
          div().render
        }
      }, style = width(5, 15, 20)),
      //TableFactory.TableAttrib("", (ar, _, _) => div("\u22EE").render, style = width(5, 5, 5)),
    )

    implicit object rowHandler extends views.TableFactory.TableRowHandler[ArticleRowModel, ArticleIdModel] {
      object commentLoader {
        val loading = mutable.HashMap.empty[ArticleIdModel, Future[Unit]]
      }

      def checkHighlight(item: ArticleRowModel, matchInfo: Seq[TextMatchIssue]): Boolean = {
        if (item.id.id.nonEmpty) { // a comment
          matchInfo.exists(
            p => p.object_type == "IssueComment" && p.object_url.contains("/" + item.id.id.get.toString)
          ) || {
            // extended highlighting - highlight all text matches, not only those returned by the TextMatchIssue
            val highlights = matchInfo.flatMap(_.matches.map(_.text)).distinct
            Highlight.isHighlighted(item.title, highlights) || Highlight.isHighlighted(item.body, highlights)
          }
        } else { // an issue
          matchInfo.exists(p => p.object_type == "Issue")
        }
      }

      override def id(item: ArticleRowModel) = item.id
      override def indent(item: ArticleRowModel) = item.indent
      override def rowModifier(itemModel: ModelProperty[ArticleRowModel]) = {
        val ar = itemModel.get
        val id = ar.id
        Seq[Modifier](
          CssStyleName("table-fold"),
          s.tr,
          rowStyle(itemModel),
          CssStyleName("custom-context-menu"),
          attr("issue-context") := id.context.relativeUrl,
          attr("issue-number") := id.issueNumber,
          attr("reply-number") := ar.replyNumber,
          attr("issue-labels") := ar.labels.map(_.name).mkString("\"", "\",\"", "\""),
          attr("issue-assignees") := ar.assignees.map(_.login).mkString("\"", "\",\"", "\""),
          if (checkHighlight(ar, ar.rawParent.text_matches.toSeq)) Seq[Modifier](s.hightlightIssue) else Seq.empty[Modifier],
          id.id.map(attr("comment-number") := _) // include only when the value is present
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
            childrenClosed.get().foreach { close =>
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
            val (data, _) = fetchElementData(tr)
            commentLoader.loading.getOrElseUpdate(data, {
              val token = presenter.props.subProp(_.token).get
              val state = presenter.filterState()
              val rowData = model.subProp(_.articles).get.find(_.id == data).get
              presenter.loadIssueComments(data, token, state, rowData.rawParent)
            }).tap {
              // once completed, remove it from the in-progress list
              _.onComplete(_ => commentLoader.loading.remove(data))
            }
          } else {
            // create a future which will never complete
            // used for simulating long loading when debugging
            Promise[Unit]().future
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
          val (data, _) = fetchElementData(tr)
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
    val repoSelected = globals.subProp(_.selectedContext).bitransform(_.getOrElse(repoUrl.get.head))(Some(_: ContextModel))

    val repoAttribs = Seq[TableFactory.TableAttrib[ContextModel]](
      TableFactory.TableAttrib(
        "", { (ar, arProp, _) =>
          val shortName = shortId(ar)
          val checked = repoSelected.transform(_ == ar)
          // inspired by io.udash.bindings.inputs.Checkbox and io.udash.bindings.inputs.RadioButtons
          Seq(
            input("", tpe := "radio").render.tap(in =>
              checked.listen(in.checked = _, initUpdate = true),
            ).tap (_.onchange = _ => repoSelected.set(ar)),
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
          jQ(t).addContextMenu(
            new Options {
              override val selector = "tr"
              override val hideOnSecondTrigger = true
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
        div(cls := "row justify-content-left")(addRepoButton),
        labelButtons,
        filterButtons,
        assignButtons
      ),
      div(
        s.gridAreaFilters,
        div(
          s.filterExpression,
          inputs.TextInput(model.subProp(_.filterExpression), 1.second)(s.filterExpressionInput)
        ).render,
        showIfElse(model.subProp(_.loading))(
          Seq.empty,
          Seq[Node](
            div(
              s.flexRow,
              nextPageButton.render,
              refreshNotifications.render,
              div(s.useFlex1).render,
              newIssueButton.render,
            ).render
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
                    override val hideOnSecondTrigger = true
                    override val build = js.defined { (item, key) =>

                      def prefixChecked(checked: Boolean) = if (checked) "<span style='position:absolute;left:1em'>" + Icons.check() + "</span>" else ""
                      def nodeHtml(node: Node) = node.render.asInstanceOf[dom.Element].outerHTML

                      val (data, replyNumber) = fetchElementData(item)
                      val itemLabels = fetchElementLabels(item)
                      val itemAssignees = fetchElementAssignees(item)
                      val collaborators = model.subProp(_.selectedContextCollaborators).get.map { u =>
                        val checked = elementAssigneesContains(itemAssignees, u.login)
                        "user-label-" + u -> BuildItem(
                          prefixChecked(checked) + nodeHtml(userHtml(u)),
                          if (checked) presenter.removeAssignee(data, u.login) else presenter.addAssignee(data, u.login),
                          isHtmlName = true
                        )
                      }
                      val labels = model.subProp(_.labels).get.map { l =>
                        val checked = elementLabelsContains(itemLabels, l.name)
                        "label-" + l.name -> BuildItem(
                          prefixChecked(checked) + nodeHtml(labelHtml(l)),
                          if (checked) presenter.removeLabel(data, l.name) else presenter.addLabel(data, l.name),
                          isHtmlName = true
                        )
                      }
                      new Build(
                        items = js.Dictionary(
                          "markAsRead" -> BuildItem(s"Mark ${data.issueIdName(shortId(data.context))} as read", presenter.markAsRead(data)),
                          "reply" -> BuildItem("Reply", presenter.reply(data), disabled = presenter.wasEditing()),
                          "labels" -> new Submenu(
                            "Labels", js.Dictionary(labels:_*)
                          ),
                          "assignees" -> new Submenu(
                            "Assigned to", js.Dictionary(collaborators:_*)
                          ),
                          "sep2" -> "------",
                          "close" -> BuildItem("Close", presenter.closeIssue(data)),
                          "sep1" -> "------",
                          "link" -> BuildItem("Copy link to " + data.issueLinkFull(shortId(data.context), replyNumber).render.outerHTML, presenter.copyLink(data), isHtmlName = true),
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
                      h4(`class` := "title", span(title), span(`class` := "link", row.id.issueLinkFull(shortId(row.id.context), row.replyNumber))),
                      div(span(`class` := "createdBy", userHtml(row.createdBy)))
                    ),
                    div(s.useFlex1),
                    div(editButton),
                    div(replyButton)
                  ).render
                case _ =>
                  div().render
              },
              showIfElse(model.subProp(_.editing).transform(_._1))(
                Seq[Node](
                  // when we are editing a new issue, show the title input
                  div(
                    showIf(model.subProp(_.selectedArticleId).transform(_.isEmpty))(
                      Seq[Node](
                        TextInput(model.subProp(_.editedArticleTitle))(s.titleEdit, id := "edit-title", placeholder := "Title").render.tap { in =>
                          jQ(in).on("keyup", (el, ev) => {
                            if (ev.key == "Enter") {
                              presenter.focusEditText()
                            } else if (ev.key == "Escape") {
                              presenter.editCancel()
                            }
                          })
                        }
                      )
                    )
                  ).render,
                  div(
                    s.editArea,
                    TextArea(model.subProp(_.editedArticleMarkdown))(Form.control, s.editTextArea, id := "edit-text-area", placeholder :="Leave a comment")
                      .render.tap { in =>
                      jQ(in).on("keyup", (el, ev) => {
                        if (ev.key == "Enter" && ev.ctrlKey) {
                          presenter.editOK()
                        } else if (ev.key == "Escape") {
                          presenter.editCancel()
                        }
                      })
                    },
                    div(
                      s.editButtons,
                      s.flexRow,
                      div(s.useFlex1),
                      editOKButton,
                      editCancelButton
                    )
                  ).render
                ),
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
      ),
      div(
        //display.none,
        addRepoModal,
      ),

    ).render
  }
}
