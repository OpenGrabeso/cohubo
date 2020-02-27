package com.github.opengrabeso.cohabo
package frontend
package views

import routing._
import io.udash._
import io.udash.bootstrap._
import io.udash.bootstrap.button.UdashButton
import io.udash.component.ComponentId
import io.udash.css._
import common.css._
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
    login()

    override def handleState(state: RootState.type): Unit = {}

    def login() = {
      val globalUserId = "A"
      val globalAuthCode = "code"
      val sessionId = "session"
      val ctx = userContextService.login(globalUserId, globalAuthCode, sessionId)
      userContextService.userName.foreach(_.foreach { name =>
        model.subProp(_.userName).set(name)
      })
      model.subProp(_.userId).set(ctx.userId)
    }

    def logout() = {
      if (model.subProp(_.userId).get != null) {
        println("Start logout")
        val oldName = userContextService.userName
        val oldId = userContextService.userId
        userContextService.logout().andThen {
          case Success(_) =>
            println(s"Logout done for $oldName ($oldId)")
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
          //GlobalStyles.header,
          id := "header",
          table(
            tbody(
              tr(
                td(
                  table(
                    tbody(
                      tr(td(a(
                        href := "/", appName,
                        onclick :+= {_: dom.Event =>
                          presenter.gotoMain()
                          true
                        }
                      ))),
                      tr(td(
                        "user:",
                        produce(userId) { s =>
                          a(href := s"https://github.com/$s", bind(name)).render
                        }
                      ))
                    )
                  )
                ),

                logoutButton.render
              )
            )
          )
        ).render
      )
    }


    val footer: Seq[HTMLElement] = Seq(
      div(
        //GlobalStyles.footer,
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
      val model = ModelProperty(PageModel(null, userService.userId.orNull))
      val presenter = new PagePresenter(model, userService, application)

      val view = new View(model, presenter)
      (view, presenter)
    }
  }

}