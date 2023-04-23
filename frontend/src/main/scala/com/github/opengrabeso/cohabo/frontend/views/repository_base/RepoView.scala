package com.github.opengrabeso.cohabo
package frontend
package views
package repository_base

import java.time.ZonedDateTime
import com.github.opengrabeso.facade
import common.css._
import io.udash._
import io.udash.bootstrap.table.UdashTable
import io.udash.css._
import scalatags.JsDom.all.{Modifier, _}
import io.udash.bootstrap._
import BootstrapStyles._
import com.github.opengrabeso.github.model._
import frontend.dataModel._
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.modal.UdashModal
import io.udash.wrappers.jquery.{JQuery, jQ}
import org.scalajs.dom.{Element, Node}

import scala.scalajs.js
import scala.concurrent.duration.{span => _, _}

trait RepoView extends View with CssView with PageUtils with TimeFormatting with CssBase with JQEvents {
  def presenter: RepoPresenter
  def selectedContextModel: Property[Option[ContextModel]]

  def globals: ModelProperty[SettingsModel]


  private val s = SelectPageStyles

  protected val settingsButton = button("Settings".toProperty)

  protected val addRepoButton = button("Add repository".toProperty, buttonStyle = BootstrapStyles.Color.Success)
  protected val addRepoInput = Property[String]("")

  protected val addRepoOkButton = UdashButton(options = BootstrapStyles.Color.Success.option)(_ => Seq[Modifier](UdashModal.CloseButtonAttr, "OK"))
    .tap(buttonOnClick(_)(presenter.addRepository(addRepoInput.get)))

  protected val addRepoModal = UdashModal(Some(Size.Small).toProperty)(
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

  buttonOnClick(settingsButton) {
    presenter.gotoSettings()
  }
  buttonOnClick(addRepoButton) {
    showRepoModal()
  }


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

  def fetchRepoData(e: JQuery): ContextModel = {
    ContextModel.parse(e.attr("repository").get)
  }


  def repoTableTemplate: UdashTable[ContextModel, CastableProperty[ContextModel]] = {

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
            ).tap(_.onchange = _ => repoSelected.set(ar)),
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
        selectedContextModel,
        repoAttribs
      )
    )

    repoTable

  }

  def repoTableRender(repoTable: UdashTable[ContextModel, CastableProperty[ContextModel]]): Element = {
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
    }

  }

}
