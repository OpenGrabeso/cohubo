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
      val settingsModel = SettingsModel.load
      userContextService.properties.set(settingsModel)
      userContextService.properties.subProp(_.user).listen { userLogin =>
        model.subProp(_.userName).set(userLogin.fullName)
        model.subProp(_.userId).set(userLogin.login)
      }
    }

    def logout() = {
      if (model.subProp(_.userId).get != null) {
        println("Start logout")
        userContextService.logout().andThen {
          case Success(_) =>
            println(s"Logout done")
            model.subProp(_.userName).set(null)
            model.subProp(_.userId).set(null)
            MainJS.deleteCookie("authCode")
            application.redirectTo("/app")
          case Failure(_) =>
        }
      }
    }

    def gotoMain() = {
      application.goTo(SelectPageState)
    }
  }


  class View(model: ModelProperty[PageModel], presenter: PagePresenter) extends ContainerView with CssView {

    import scalatags.JsDom.all._

    private val logoutButton = UdashButton(
      //buttonStyle = ButtonStyle.Default,
      block = true.toProperty, componentId = ComponentId("logout-button")
    )("Log Out")

    logoutButton.listen {
      case UdashButton.ButtonClickEvent(_, _) =>
        println("Logout submit pressed")
        presenter.logout()
    }


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
            `class`:="ml-auto px-2",
            GlobalStyles.logoutButton,
            logoutButton
          )
        ).render
      )
    }


    val footer: Seq[HTMLElement] = Seq(
      div(
        Spacing.margin(size = SpacingSize.Small),
        id := "footer",
        /*
        a(
          href := "http://labs.strava.com/",
          id := "powered_by_strava",
          rel := "nofollow",
          img(
            GlobalStyles.stravaImg,
            attr("align") := "left",
            src :="static/api_logo_pwrdBy_strava_horiz_white.png",
          )
        ),
       */
        p(
          GlobalStyles.footerText,
          " © 2020 ",
          a(
            href := s"https://github.com/OndrejSpanel/$gitHubName",
            GlobalStyles.footerLink,
            "Ondřej Španěl"
          ),
          div()
        )
      ).render
    )

    // ContainerView contains default implementation of child view rendering
    // It puts child view into `childViewContainer`
    override def getTemplate: Modifier = div(
      // loads Bootstrap and FontAwesome styles from CDN
      UdashBootstrap.loadBootstrapStyles(),
      UdashBootstrap.loadFontAwesome(),
      BootstrapStyles.container,
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

      val view = new View(model, presenter)
      (view, presenter)
    }
  }

}