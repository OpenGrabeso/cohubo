package com.github.opengrabeso.cohabo.frontend
package routing

import views._
import io.udash._

class StatesToViewFactoryDef extends ViewFactoryRegistry[RoutingState] {
  def matchStateToResolver(state: RoutingState): ViewFactory[_ <: RoutingState] =
    state match {
      case RootState => new Root.PageViewFactory(ApplicationContext.application, ApplicationContext.userContextService)
      case SelectPageState(_) => select.PageViewFactory
      case SettingsPageState => new settings.PageViewFactory(ApplicationContext.application, ApplicationContext.userContextService)
    }
}