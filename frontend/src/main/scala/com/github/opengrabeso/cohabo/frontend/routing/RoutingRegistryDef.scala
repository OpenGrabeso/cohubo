package com.github.opengrabeso.cohabo
package frontend
package routing

import dataModel._
import io.udash._

import scala.scalajs.js.URIUtils

class RoutingRegistryDef extends RoutingRegistry[RoutingState] {
  def matchUrl(url: Url): RoutingState =
    url2State("/" + url.value.stripPrefix("/").stripSuffix("/"))

  def matchState(state: RoutingState): Url = Url(state2Url(state))

  object URIEncoded {
    def apply(s: String): String = URIUtils.encodeURIComponent(s)
    def unapply(s: String): Option[String] = Some(URIUtils.decodeURIComponent(s))
  }
  object IdPar {
    def apply(id: ArticleIdModel) = {
      "/" + id.toUrlString
    }
    def unapply(s: String): Option[ArticleIdModel] = {
      if (s.startsWith("/")) {
        ArticleIdModel.parse(s.drop(1))
      } else None
    }
  }
  private val (url2State, state2Url) = bidirectional {
    case "/settings" => SettingsPageState
    case "/" => SelectPageState(None)
    case IdPar(id) => SelectPageState(Some(id))
  }
}