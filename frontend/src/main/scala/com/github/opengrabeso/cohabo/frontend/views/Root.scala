package com.github.opengrabeso.cohabo
package frontend
package views

import com.github.opengrabeso.cohabo.frontend.dataModel.SettingsModel
import rest.AuthorizedAPI
import routing._
import io.udash._
import io.udash.bootstrap._
import io.udash.bootstrap.button.UdashButton
import io.udash.component.ComponentId
import io.udash.css._
import common.css._
import io.udash.bootstrap.utils.BootstrapStyles._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Root {

  case class PageModel(userName: String, userId: String)

  object PageModel extends HasModelPropertyCreator[PageModel]

  class PagePresenter(
    model: ModelProperty[PageModel],
    userContextService: services.UserContextService,
    application: Application[RoutingState]
  )(implicit ec: ExecutionContext) extends Presenter[RootState.type] {

    // start the login
    init()

    override def handleState(state: RootState.type): Unit = {}

    def init(): Unit = {
      userContextService.properties.subProp(_.user).listen { userLogin =>
        model.subProp(_.userName).set(userLogin.fullName)
        model.subProp(_.userId).set(userLogin.login)
      }
    }

    def gotoMain() = {
      application.goTo(SelectPageState)
    }
  }


  class View(
    model: ModelProperty[PageModel],
    globals: ModelProperty[SettingsModel],
    presenter: PagePresenter
  ) extends ContainerView with CssView {

    import scalatags.JsDom.all._

    val header: Seq[HTMLElement] = {
      val name = model.subProp(_.userName)
      val userId = model.subProp(_.userId)

      Seq(
        div(
          id := "header",
          div(
            a(
              Spacing.margin(size = SpacingSize.Small),
              href := "/", appName,
              onclick :+= {_: dom.Event =>
                presenter.gotoMain()
                true
              }
            )
          ),
          div(
            Spacing.margin(size = SpacingSize.Small),
            "User:",
            produce(userId) { s =>
              a(
                Spacing.margin(size = SpacingSize.Small),
                href := s"https://github.com/$s", bind(name)
              ).render
            }
          ),
          div(
            produce(globals.subProp(_.rateLimits)) {
              case Some((limit, remaining, reset)) =>
                val now = System.currentTimeMillis() / 1000
                s"Remaining $remaining of $limit, reset in ${(reset - now) / 60} min".render
              case None =>
                "".render
            }
          )
        ).render
      )
    }


    val footer: Seq[HTMLElement] = Seq(
      div(
        Spacing.margin(size = SpacingSize.Small),
        id := "footer",
        a(
          href := "https://www.github.com",
          id := "powered_by_github",
          rel := "nofollow",
          i(cls := "fab fa-github"),
          "Powered by GitHub"
        ),
        div(
          GlobalStyles.useFlex1,
        ),
        p(
          GlobalStyles.footerText,
          " © 2020 ",
          a(
            href := s"https://github.com/OndrejSpanel/$gitHubName",
            GlobalStyles.footerLink,
            "Ondřej Španěl"
          )
        )
      ).render
    )

    // ContainerView contains default implementation of child view rendering
    // It puts child view into `childViewContainer`
    override def getTemplate: Modifier = div(
      // loads Bootstrap and FontAwesome styles from CDN
      UdashBootstrap.loadBootstrapStyles(),
      UdashBootstrap.loadFontAwesome(),
      GlobalStyles.rootContainer,
      header,
      childViewContainer,
      footer
    )
  }

  class PageViewFactory(
    application: Application[RoutingState],
    userService: services.UserContextService,
  ) extends ViewFactory[RootState.type] {

    import scala.concurrent.ExecutionContext.Implicits.global

    override def create(): (View, Presenter[RootState.type]) = {
      val model = ModelProperty(PageModel(null, null))
      val presenter = new PagePresenter(model, userService, application)

      val view = new View(model, userService.properties, presenter)
      (view, presenter)
    }
  }

}